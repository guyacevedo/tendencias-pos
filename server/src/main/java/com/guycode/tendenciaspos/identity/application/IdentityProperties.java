package com.guycode.tendenciaspos.identity.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identidad (prefijo {@code tpos.identity}).
 *
 * @param accessTokenTtl vida del access token JWT
 * @param refreshTokenTtl duración máxima de una sesión; renovar no la extiende
 * @param adminInitialPassword clave del primer administrador ({@code ADMIN_INITIAL_PASSWORD}); solo se usa
 *     si no hay usuarios
 */
@ConfigurationProperties("tpos.identity")
public record IdentityProperties(Duration accessTokenTtl, Duration refreshTokenTtl, String adminInitialPassword) {
    public IdentityProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofHours(8);
        }
    }

    @Override
    public String toString() {
        return "IdentityProperties[accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl
                + ", adminInitialPassword=***]";
    }
}
