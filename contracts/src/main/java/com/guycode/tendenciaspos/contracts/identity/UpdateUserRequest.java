package com.guycode.tendenciaspos.contracts.identity;

import java.util.Set;

/** {@code PUT /api/users/{id}}: nombre y roles. La clave y el estado tienen sus propias operaciones. */
public record UpdateUserRequest(String fullName, Set<UserRole> roles) {}
