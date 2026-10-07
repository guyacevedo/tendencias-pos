package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.LockoutPolicy;
import com.guycode.tendenciaspos.identity.domain.PasswordPolicy;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.identity.domain.Username;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.shared.web.ErrorCode;
import java.time.Clock;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ingreso, renovación y cierre de sesión, y cambio de la clave propia. */
@Service
public class AuthService {
    private static final String USER = "USER";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder encoder;
    private final AuditLog audit;
    private final Clock clock;
    private final LockoutPolicy lockout = LockoutPolicy.DEFAULT;
    /** Hash señuelo: un usuario inexistente cuesta lo mismo que una clave errada (no revela qué usuarios existen). */
    private final String dummyHash;

    AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            TokenService tokens,
            PasswordEncoder encoder,
            AuditLog audit,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = encoder.encode("clave-señuelo-que-nadie-usa");
    }

    @Transactional
    public LoginResult login(String rawUsername, String password, String clientIp) {
        var username = Username.normalize(rawUsername);
        clientIp = clientIp == null ? "?" : clientIp;
        var pass = password == null ? "" : password;
        var now = clock.instant();
        var found = users.findByUsernameForUpdate(username);
        if (found.isEmpty() || !found.get().active()) {
            encoder.matches(pass, dummyHash);
            audit.record(username, "LOGIN_FAILED", USER, null, Map.of("ip", clientIp, "reason", "unknown_or_inactive"));
            return new LoginResult.InvalidCredentials();
        }
        var user = found.get();
        if (user.isLocked(now)) {
            audit.record(username, "LOGIN_REJECTED_LOCKED", USER, user.id(), Map.of("ip", clientIp));
            return new LoginResult.Locked(user.lockedUntil());
        }
        if (!encoder.matches(pass, user.passwordHash())) {
            boolean lockedNow = user.registerFailedLogin(now, lockout);
            users.save(user);
            audit.record(username, "LOGIN_FAILED", USER, user.id(), Map.of("ip", clientIp, "reason", "bad_password"));
            if (lockedNow) {
                audit.record(username, "ACCOUNT_LOCKED", USER, user.id(), Map.of("until", user.lockedUntil()));
                return new LoginResult.Locked(user.lockedUntil());
            }
            return new LoginResult.InvalidCredentials();
        }
        user.registerSuccessfulLogin(now);
        if (encoder.upgradeEncoding(user.passwordHash())) {
            user.upgradePasswordHash(encoder.encode(pass));
        }
        users.save(user);
        audit.record(username, "LOGIN_SUCCESS", USER, user.id(), Map.of("ip", clientIp));
        return new LoginResult.Success(tokens.startSession(user, now));
    }

    /**
     * Cambia el refresh token por un par nuevo que vence cuando vencía la sesión. Presentar un token ya usado
     * indica robo o un cliente con fallas: se cierran todas las sesiones del usuario.
     */
    @Transactional
    public RefreshResult refresh(String refreshToken, String clientIp) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return new RefreshResult.Rejected();
        }
        clientIp = clientIp == null ? "?" : clientIp;
        var now = clock.instant();
        var found = refreshTokens.findByHashForUpdate(TokenService.hash(refreshToken));
        if (found.isEmpty()) {
            return new RefreshResult.Rejected();
        }
        var stored = found.get();
        if (stored.isRevoked()) {
            int revoked = refreshTokens.revokeAllForUser(stored.userId(), now);
            audit.record(
                    users.findById(stored.userId()).map(User::username).orElse(null),
                    "REFRESH_TOKEN_REUSED",
                    USER,
                    stored.userId(),
                    Map.of("ip", clientIp, "sessionsRevoked", revoked));
            return new RefreshResult.Rejected();
        }
        if (stored.isExpired(now)) {
            return new RefreshResult.Rejected();
        }
        var user = users.findById(stored.userId()).filter(User::active);
        stored.revoke(now);
        refreshTokens.save(stored);
        if (user.isEmpty()) {
            return new RefreshResult.Rejected();
        }
        return new RefreshResult.Success(tokens.issue(user.get(), now, stored.expiresAt()));
    }

    /** Revoca la sesión del refresh token. Idempotente: un token desconocido no es error. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        var now = clock.instant();
        refreshTokens
                .findByHashForUpdate(TokenService.hash(refreshToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> {
                    t.revoke(now);
                    refreshTokens.save(t);
                    var username =
                            users.findById(t.userId()).map(User::username).orElse(null);
                    audit.record(username, "LOGOUT", USER, t.userId(), null);
                });
    }

    /** El usuario cambia su clave; se cierran sus otras sesiones y se devuelve una sesión nueva. */
    @Transactional
    public IssuedSession changePassword(Actor actor, String currentPassword, String newPassword) {
        var now = clock.instant();
        var user = users.findById(actor.userId())
                .filter(User::active)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (currentPassword == null || !encoder.matches(currentPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        if (!PasswordPolicy.isAcceptable(newPassword)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
        if (encoder.matches(newPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD, "La nueva clave debe ser diferente de la actual.");
        }
        user.changePassword(encoder.encode(newPassword), now);
        users.save(user);
        refreshTokens.revokeAllForUser(user.id(), now);
        audit.record(user.username(), "PASSWORD_CHANGED", USER, user.id(), null);
        return tokens.startSession(user, now);
    }

    @Transactional(readOnly = true)
    public User currentUser(Actor actor) {
        return users.findById(actor.userId())
                .filter(User::active)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
