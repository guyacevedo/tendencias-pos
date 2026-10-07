package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.ChangePasswordRequest;
import com.guycode.tendenciaspos.contracts.identity.LoginRequest;
import com.guycode.tendenciaspos.contracts.identity.RefreshTokenRequest;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import java.util.Set;

/** Endpoints de sesión ({@code /api/auth}). */
public final class AuthApi implements SessionGateway {
    public static final String LOGIN_PATH = "/api/auth/login";
    public static final String REFRESH_PATH = "/api/auth/refresh";
    public static final String LOGOUT_PATH = "/api/auth/logout";
    public static final String ME_PATH = "/api/auth/me";
    public static final String PASSWORD_PATH = "/api/auth/password";

    /** Rutas que no llevan access token (y que por eso no disparan renovaciones). */
    public static final Set<String> PUBLIC_PATHS = Set.of(LOGIN_PATH, REFRESH_PATH, LOGOUT_PATH);

    private final ApiClient client;

    public AuthApi(ApiClient client) {
        this.client = client;
    }

    public AuthTokens login(String username, String password) {
        return client.post(LOGIN_PATH, new LoginRequest(username, password), AuthTokens.class);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        return client.post(REFRESH_PATH, new RefreshTokenRequest(refreshToken), AuthTokens.class);
    }

    @Override
    public void logout(String refreshToken) {
        client.post(LOGOUT_PATH, new RefreshTokenRequest(refreshToken), Void.class);
    }

    public SessionUser me() {
        return client.get(ME_PATH, SessionUser.class);
    }

    /** Devuelve una sesión nueva: cambiar la clave revoca las anteriores. */
    public AuthTokens changePassword(String currentPassword, String newPassword) {
        return client.post(PASSWORD_PATH, new ChangePasswordRequest(currentPassword, newPassword), AuthTokens.class);
    }
}
