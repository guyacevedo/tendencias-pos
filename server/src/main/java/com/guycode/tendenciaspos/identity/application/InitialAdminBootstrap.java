package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.PasswordPolicy;
import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import java.time.Clock;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Si no hay ningún usuario, crea {@code admin} con la clave de {@code ADMIN_INITIAL_PASSWORD}; deberá cambiarla
 * en su primer ingreso. Sin esa variable (o con una clave corta) la API no arranca, para no quedar sin acceso.
 */
@Component
class InitialAdminBootstrap implements ApplicationRunner {
    static final String USERNAME = "admin";
    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final IdentityProperties props;
    private final AuditLog audit;
    private final Clock clock;

    InitialAdminBootstrap(
            UserRepository users, PasswordEncoder encoder, IdentityProperties props, AuditLog audit, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        var password = props.adminInitialPassword();
        if (!PasswordPolicy.isAcceptable(password)) {
            throw new IllegalStateException("No hay usuarios: defina ADMIN_INITIAL_PASSWORD con "
                    + PasswordPolicy.MIN_LENGTH + " a " + PasswordPolicy.MAX_LENGTH + " caracteres");
        }
        var admin = users.save(
                User.create(USERNAME, "Administrador", encoder.encode(password), Set.of(Role.ADMIN), clock.instant()));
        audit.record("system", "USER_CREATED", "USER", admin.id(), null);
        log.info("Administrador inicial '{}' creado; debe cambiar la clave en el primer ingreso", USERNAME);
    }
}
