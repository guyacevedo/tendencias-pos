package com.guycode.tendenciaspos.identity.application;

/** Resultado de renovar la sesión. No es excepción para que la revocación por reuso quede guardada. */
public sealed interface RefreshResult {
    record Success(IssuedSession session) implements RefreshResult {}

    record Rejected() implements RefreshResult {}
}
