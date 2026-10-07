package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;

/** Puerto de salida: tokens de renovación. */
public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);

    /** Busca por hash y bloquea la fila: dos renovaciones simultáneas del mismo token no pueden ganar ambas. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    /** Revoca todas las sesiones abiertas del usuario. */
    int revokeAllForUser(long userId, Instant now);
}
