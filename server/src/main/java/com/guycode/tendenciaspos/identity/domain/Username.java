package com.guycode.tendenciaspos.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Los nombres de usuario se guardan en minúsculas: "Ana" y "ana" son el mismo usuario. */
public final class Username {
    private static final Pattern VALID = Pattern.compile("[a-z0-9._-]{3,50}");

    private Username() {}

    /** Quita espacios y pasa a minúsculas; {@code null} se vuelve cadena vacía. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    }

    /** 3 a 50 caracteres: letras sin tilde, números, punto, guion o guion bajo. */
    public static boolean isValid(String normalized) {
        return VALID.matcher(normalized).matches();
    }
}
