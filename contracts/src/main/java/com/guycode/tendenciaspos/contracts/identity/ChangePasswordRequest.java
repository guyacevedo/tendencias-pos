package com.guycode.tendenciaspos.contracts.identity;

/** {@code POST /api/auth/password}: el usuario cambia su propia clave. */
public record ChangePasswordRequest(String currentPassword, String newPassword) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
