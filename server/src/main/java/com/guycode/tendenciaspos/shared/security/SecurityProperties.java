package com.guycode.tendenciaspos.shared.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seguridad de la API (prefijo {@code tpos.security}).
 *
 * @param jwtSecret clave HS256 de los access tokens ({@code JWT_SECRET}); al menos 32 bytes
 */
@ConfigurationProperties("tpos.security")
public record SecurityProperties(String jwtSecret) {
    static final int MIN_SECRET_BYTES = 32;

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "tpos.security.jwt-secret (JWT_SECRET) debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
    }

    /** Clave HMAC-SHA256 derivada del secreto. */
    public SecretKey signingKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String toString() {
        return "SecurityProperties[jwtSecret=***]";
    }
}
