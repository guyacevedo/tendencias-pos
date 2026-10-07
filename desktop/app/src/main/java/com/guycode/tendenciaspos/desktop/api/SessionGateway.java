package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;

/** Operaciones de sesión que usa el administrador de sesión para renovar y cerrar. */
public interface SessionGateway {
    AuthTokens refresh(String refreshToken);

    void logout(String refreshToken);
}
