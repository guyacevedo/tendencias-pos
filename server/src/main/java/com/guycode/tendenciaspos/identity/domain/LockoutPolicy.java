package com.guycode.tendenciaspos.identity.domain;

import java.time.Duration;

/**
 * Bloqueo temporal ante intentos fallidos de ingreso.
 *
 * @param maxFailedAttempts intentos fallidos seguidos que bloquean la cuenta
 * @param lockDuration tiempo que dura el bloqueo
 */
public record LockoutPolicy(int maxFailedAttempts, Duration lockDuration) {
    /** 5 intentos, 15 minutos. */
    public static final LockoutPolicy DEFAULT = new LockoutPolicy(5, Duration.ofMinutes(15));
}
