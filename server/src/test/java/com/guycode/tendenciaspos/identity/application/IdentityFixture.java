package com.guycode.tendenciaspos.identity.application;

import static org.mockito.Mockito.mock;

import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import com.guycode.tendenciaspos.shared.security.TokenClaims;
import com.guycode.tendenciaspos.support.MutableClock;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Servicios de identidad reales sobre repositorios en memoria y un reloj controlado. */
class IdentityFixture {
    static final Instant T0 = Instant.parse("2026-10-06T12:00:00Z");
    /** Argon2 liviano: mismo algoritmo, pruebas rápidas. */
    static final PasswordEncoder ENCODER = new Argon2PasswordEncoder(16, 32, 1, 1 << 10, 1);

    private static final SecretKeySpec KEY = new SecretKeySpec(
            "secreto-de-pruebas-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    final MutableClock clock = new MutableClock(T0);
    final InMemoryUsers users = new InMemoryUsers();
    final InMemoryRefreshTokens refreshTokens = new InMemoryRefreshTokens();
    final AuditLog audit = mock(AuditLog.class);
    final IdentityProperties props = new IdentityProperties(Duration.ofMinutes(15), Duration.ofHours(8), null);
    final TokenService tokens =
            new TokenService(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(KEY)), refreshTokens, props);
    final AuthService auth = new AuthService(users, refreshTokens, tokens, ENCODER, audit, clock);
    final UserAdminService admin = new UserAdminService(users, refreshTokens, ENCODER, audit, clock);
    final JwtDecoder decoder = decoder();

    /** Verifica la firma sin validar fechas (el reloj de la prueba no es el real). */
    private static JwtDecoder decoder() {
        var decoder = NimbusJwtDecoder.withSecretKey(KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new JwtIssuerValidator(TokenClaims.ISSUER));
        return decoder;
    }

    /** Usuario que ya cambió su clave. */
    User existing(String username, String password, Role... roles) {
        var user = User.create(username, username.toUpperCase(), ENCODER.encode(password), Set.of(roles), T0);
        user.changePassword(ENCODER.encode(password), T0);
        return users.save(user);
    }
}
