package com.guycode.tendenciaspos.contracts.identity;

/** {@code POST /api/auth/refresh} y {@code POST /api/auth/logout}. */
public record RefreshTokenRequest(String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
