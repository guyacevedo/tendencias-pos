package com.guycode.tendenciaspos.support;

import com.guycode.tendenciaspos.shared.security.TokenClaims;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Emite access tokens de prueba firmados con el mismo secreto que valida la API. */
public class TestTokens {
    public static final String SECRET = "secreto-de-pruebas-de-al-menos-32-bytes!!";

    private final JwtEncoder encoder;

    public TestTokens(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String admin() {
        return token(1, "admin", List.of("ADMIN"), false, Instant.now());
    }

    public String cashier() {
        return token(2, "cajero", List.of("CASHIER"), false, Instant.now());
    }

    public String adminMustChangePassword() {
        return token(1, "admin", List.of("ADMIN"), true, Instant.now());
    }

    /** Emitido hace una hora: vencido aunque se tolere el desfase de reloj. */
    public String expired() {
        return token(1, "admin", List.of("ADMIN"), false, Instant.now().minus(Duration.ofHours(1)));
    }

    public String token(long userId, String username, List<String> roles, boolean mustChange, Instant issuedAt) {
        var claims = JwtClaimsSet.builder()
                .issuer(TokenClaims.ISSUER)
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .claim(TokenClaims.USER_ID, userId)
                .claim(TokenClaims.ROLES, roles)
                .claim(TokenClaims.PASSWORD_CHANGE, mustChange)
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
