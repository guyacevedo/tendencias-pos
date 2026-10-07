package com.guycode.tendenciaspos.identity.domain;

/** Reglas de las claves. El tope evita entradas enormes al hash (Argon2 es costoso a propósito). */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {}

    public static boolean isAcceptable(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        return length >= MIN_LENGTH && length <= MAX_LENGTH && !rawPassword.isBlank();
    }
}
