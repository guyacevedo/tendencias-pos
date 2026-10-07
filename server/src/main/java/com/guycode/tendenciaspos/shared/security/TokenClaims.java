package com.guycode.tendenciaspos.shared.security;

/** Nombres de los claims del access token, compartidos entre quien lo emite y quien lo valida. */
public final class TokenClaims {
    /** Emisor de todos los tokens de esta API. */
    public static final String ISSUER = "tendencias-pos";
    /** Id numérico del usuario ({@code sub} lleva el nombre de usuario). */
    public static final String USER_ID = "uid";
    /** Roles sin prefijo: {@code ADMIN}, {@code CASHIER}. */
    public static final String ROLES = "roles";
    /** {@code true} mientras el usuario deba cambiar su clave; limita la API a las rutas de cuenta. */
    public static final String PASSWORD_CHANGE = "pwd_change";

    private TokenClaims() {}
}
