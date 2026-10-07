package com.guycode.tendenciaspos.contracts.identity;

import java.util.Set;

/**
 * Usuario de la sesión actual ({@code GET /api/auth/me} y respuesta del login).
 *
 * @param passwordChangeRequired si es {@code true}, la API solo acepta el cambio de clave
 */
public record SessionUser(
        long id, String username, String fullName, Set<UserRole> roles, boolean passwordChangeRequired) {
    public SessionUser {
        roles = Set.copyOf(roles);
    }
}
