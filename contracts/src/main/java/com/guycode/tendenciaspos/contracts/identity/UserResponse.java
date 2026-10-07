package com.guycode.tendenciaspos.contracts.identity;

import java.time.Instant;
import java.util.Set;

/**
 * Usuario en la administración de usuarios.
 *
 * @param lockedUntil fin del bloqueo por intentos fallidos, o {@code null} si no está bloqueado
 */
public record UserResponse(
        long id,
        String username,
        String fullName,
        Set<UserRole> roles,
        boolean active,
        Instant lockedUntil,
        boolean passwordChangeRequired,
        Instant createdAt,
        Instant updatedAt) {
    public UserResponse {
        roles = Set.copyOf(roles);
    }
}
