package com.guycode.tendenciaspos.identity.application;

import java.time.Instant;

/** Resultado de un intento de ingreso. No es excepción para que el intento fallido quede guardado. */
public sealed interface LoginResult {
    record Success(IssuedSession session) implements LoginResult {}

    /** Usuario inexistente, inactivo o clave errada: indistinguibles a propósito. */
    record InvalidCredentials() implements LoginResult {}

    record Locked(Instant lockedUntil) implements LoginResult {}
}
