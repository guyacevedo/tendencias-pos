package com.guycode.tendenciaspos.contracts.identity;

import java.util.Set;

/** {@code POST /api/users}. El usuario creado debe cambiar la clave en su primer ingreso. */
public record CreateUserRequest(String username, String fullName, String password, Set<UserRole> roles) {
    @Override
    public String toString() {
        return "CreateUserRequest[username=" + username + ", fullName=" + fullName + ", roles=" + roles
                + ", password=***]";
    }
}
