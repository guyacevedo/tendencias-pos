package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.User;
import java.time.Instant;

/** Tokens recién emitidos; el refresh token va en claro solo en esta respuesta. */
public record IssuedSession(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        User user) {
    @Override
    public String toString() {
        return "IssuedSession[user=" + user.username() + ", refreshTokenExpiresAt=" + refreshTokenExpiresAt + "]";
    }
}
