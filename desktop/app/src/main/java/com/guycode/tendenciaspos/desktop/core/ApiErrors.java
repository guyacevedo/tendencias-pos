package com.guycode.tendenciaspos.desktop.core;

import com.guycode.tendenciaspos.desktop.api.ApiException;

/** Traduce los errores de la API a mensajes para el usuario. */
public final class ApiErrors {
    public static final String NO_CONNECTION = "No se pudo contactar al servidor.";
    public static final String UNEXPECTED = "Ocurrió un error inesperado.";

    private ApiErrors() {}

    public static String messageFor(Exception error) {
        if (error instanceof ApiException api) {
            return api.isConnectivity() ? NO_CONNECTION : api.getMessage();
        }
        return UNEXPECTED;
    }
}
