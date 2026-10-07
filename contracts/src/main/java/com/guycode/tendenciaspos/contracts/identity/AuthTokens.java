package com.guycode.tendenciaspos.contracts.identity;

import java.time.Instant;

/**
 * Par de tokens de una sesión. El access token (JWT) va en {@code Authorization: Bearer}; el refresh token es
 * de un solo uso: cada renovación devuelve uno nuevo y el anterior deja de servir.
 *
 * @param refreshTokenExpiresAt fin de la sesión; no se extiende al renovar
 */
public record AuthTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        SessionUser user) {
    @Override
    public String toString() {
        return "AuthTokens[accessTokenExpiresAt=" + accessTokenExpiresAt + ", refreshTokenExpiresAt="
                + refreshTokenExpiresAt + ", user=" + user + "]";
    }
}
