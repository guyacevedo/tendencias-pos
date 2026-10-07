package com.guycode.tendenciaspos.contracts.identity;

/** {@code POST /api/auth/login}. */
public record LoginRequest(String username, String password) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
