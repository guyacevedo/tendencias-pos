package com.guycode.tendenciaspos.desktop.core;

/** Comparación de versiones {@code mayor.menor.parche}; ignora sufijos como {@code -SNAPSHOT}. */
public final class Versions {
    private Versions() {}

    /** {@code true} si {@code version} es anterior a {@code minimum}. */
    public static boolean isOlder(String version, String minimum) {
        var a = parts(version);
        var b = parts(minimum);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x < y;
            }
        }
        return false;
    }

    private static int[] parts(String version) {
        var core = version.strip().split("[-+]", 2)[0];
        var pieces = core.split("\\.");
        var result = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            result[i] = pieces[i].chars().allMatch(Character::isDigit) && !pieces[i].isEmpty()
                    ? Integer.parseInt(pieces[i])
                    : 0;
        }
        return result;
    }
}
