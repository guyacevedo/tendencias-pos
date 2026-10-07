package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.PasswordPolicy;
import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.identity.domain.Username;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.shared.web.ErrorCode;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administración de usuarios (solo ADMIN; el permiso lo exige el controlador). */
@Service
public class UserAdminService {
    private static final String USER = "USER";
    private static final int FULL_NAME_MAX = 120;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuditLog audit;
    private final Clock clock;

    UserAdminService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder encoder,
            AuditLog audit,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAll();
    }

    @Transactional(readOnly = true)
    public User get(long id) {
        return find(id);
    }

    @Transactional
    public User create(Actor actor, String rawUsername, String fullName, String password, Set<Role> roles) {
        var username = Username.normalize(rawUsername);
        var errors = new LinkedHashMap<String, String>();
        if (!Username.isValid(username)) {
            errors.put("username", "De 3 a 50 caracteres: letras sin tilde, números, punto, guion o guion bajo.");
        }
        validateProfile(fullName, roles, errors);
        if (!errors.isEmpty()) {
            throw BusinessException.validation(errors);
        }
        requireAcceptable(password);
        if (users.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }
        var user =
                users.save(User.create(username, fullName.strip(), encoder.encode(password), roles, clock.instant()));
        audit.record(
                actor.username(),
                "USER_CREATED",
                USER,
                user.id(),
                Map.of("username", username, "roles", sorted(roles)));
        return user;
    }

    @Transactional
    public User update(Actor actor, long id, String fullName, Set<Role> roles) {
        var errors = new LinkedHashMap<String, String>();
        validateProfile(fullName, roles, errors);
        if (!errors.isEmpty()) {
            throw BusinessException.validation(errors);
        }
        var user = find(id);
        if (user.isActiveAdmin() && !roles.contains(Role.ADMIN)) {
            requireAnotherActiveAdmin();
        }
        user.update(fullName.strip(), roles, clock.instant());
        var saved = users.save(user);
        audit.record(
                actor.username(),
                "USER_UPDATED",
                USER,
                id,
                Map.of("fullName", saved.fullName(), "roles", sorted(roles)));
        return saved;
    }

    /** Asigna una clave temporal, desbloquea la cuenta y cierra sus sesiones. */
    @Transactional
    public void resetPassword(Actor actor, long id, String newPassword) {
        requireAcceptable(newPassword);
        var now = clock.instant();
        var user = find(id);
        user.resetPassword(encoder.encode(newPassword), now);
        users.save(user);
        refreshTokens.revokeAllForUser(id, now);
        audit.record(actor.username(), "USER_PASSWORD_RESET", USER, id, null);
    }

    /** Desactiva y cierra sus sesiones; los access tokens ya emitidos vencen solos en minutos. */
    @Transactional
    public User deactivate(Actor actor, long id) {
        if (actor.userId() == id) {
            throw new BusinessException(ErrorCode.CONFLICT, "No puede desactivar su propio usuario.");
        }
        var now = clock.instant();
        var user = find(id);
        if (user.isActiveAdmin()) {
            requireAnotherActiveAdmin();
        }
        user.deactivate(now);
        var saved = users.save(user);
        refreshTokens.revokeAllForUser(id, now);
        audit.record(actor.username(), "USER_DEACTIVATED", USER, id, null);
        return saved;
    }

    @Transactional
    public User activate(Actor actor, long id) {
        var user = find(id);
        user.activate(clock.instant());
        var saved = users.save(user);
        audit.record(actor.username(), "USER_ACTIVATED", USER, id, null);
        return saved;
    }

    private User find(long id) {
        return users.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "El usuario no existe."));
    }

    private void requireAnotherActiveAdmin() {
        if (users.countActiveAdmins() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN);
        }
    }

    private static void requireAcceptable(String password) {
        if (!PasswordPolicy.isAcceptable(password)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
    }

    private static void validateProfile(String fullName, Set<Role> roles, Map<String, String> errors) {
        if (fullName == null || fullName.isBlank()) {
            errors.put("fullName", "El nombre es obligatorio.");
        } else if (fullName.strip().length() > FULL_NAME_MAX) {
            errors.put("fullName", "Máximo " + FULL_NAME_MAX + " caracteres.");
        }
        if (roles == null || roles.isEmpty()) {
            errors.put("roles", "Asigne al menos un rol.");
        }
    }

    private static List<String> sorted(Set<Role> roles) {
        return roles.stream().map(Enum::name).sorted().toList();
    }
}
