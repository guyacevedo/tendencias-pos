package com.guycode.tendenciaspos.contracts.identity;

/** {@code POST /api/users/{id}/password}: clave temporal que el usuario deberá cambiar al entrar. */
public record ResetPasswordRequest(String newPassword) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
