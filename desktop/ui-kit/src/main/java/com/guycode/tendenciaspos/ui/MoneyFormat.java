package com.guycode.tendenciaspos.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formato de pesos colombianos para la interfaz: punto como separador de miles y coma decimal
 * ({@code 1.234.567} o {@code 1.234,50}). Sin dependencias de Swing para poder probarlo.
 */
public final class MoneyFormat {
    /** Dígitos enteros admitidos: {@code NUMERIC(14,2)} deja 12. */
    public static final int MAX_INTEGER_DIGITS = 12;

    public static final int SCALE = 2;

    /** Texto normalizado y posición del cursor tras una edición. */
    public record Edit(String text, int caret) {}

    private MoneyFormat() {}

    /** {@code null} → texto vacío. Los centavos solo se muestran si existen. */
    public static String format(BigDecimal value) {
        if (value == null) {
            return "";
        }
        var scaled = value.setScale(SCALE, RoundingMode.HALF_UP);
        var plain = scaled.abs().toPlainString();
        var dot = plain.indexOf('.');
        var integer = group(plain.substring(0, dot));
        var decimals = plain.substring(dot + 1);
        var sign = scaled.signum() < 0 ? "-" : "";
        return sign + (decimals.equals("00") ? integer : integer + "," + decimals);
    }

    /** Texto vacío o sin dígitos → {@code null}. Siempre devuelve escala 2. */
    public static BigDecimal parse(String text) {
        if (text == null) {
            return null;
        }
        var clean = normalize(text, text.length()).text().replace(".", "");
        if (clean.isEmpty()) {
            return null;
        }
        var comma = clean.indexOf(',');
        var integer = comma < 0 ? clean : clean.substring(0, comma);
        var decimals = comma < 0 ? "" : clean.substring(comma + 1);
        if (integer.isEmpty()) {
            integer = "0";
        }
        return new BigDecimal(integer + "." + (decimals.isEmpty() ? "0" : decimals)).setScale(SCALE);
    }

    /**
     * Normaliza lo que el usuario escribió: deja dígitos y una coma, máximo 12 enteros y 2 decimales,
     * quita ceros a la izquierda y agrupa miles. El cursor conserva su lugar respecto a los dígitos.
     */
    public static Edit normalize(String raw, int caret) {
        var kept = new StringBuilder();
        int keptBeforeCaret = 0;
        boolean comma = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            boolean keep = Character.isDigit(ch) && ch < 128 || (ch == ',' && !comma);
            if (keep) {
                comma |= ch == ',';
                kept.append(ch);
                if (i < caret) {
                    keptBeforeCaret++;
                }
            }
        }
        var commaAt = kept.indexOf(",");
        var integer = commaAt < 0 ? kept.toString() : kept.substring(0, commaAt);
        var decimals = commaAt < 0 ? null : kept.substring(commaAt + 1);

        int zeros = 0;
        while (zeros < integer.length() - 1 && integer.charAt(zeros) == '0') {
            zeros++;
        }
        if (decimals == null && integer.equals("0")) {
            zeros = 0;
        }
        integer = integer.substring(zeros);
        keptBeforeCaret = Math.max(0, keptBeforeCaret - zeros);
        if (integer.length() > MAX_INTEGER_DIGITS) {
            integer = integer.substring(0, MAX_INTEGER_DIGITS);
        }
        if (decimals != null && decimals.length() > SCALE) {
            decimals = decimals.substring(0, SCALE);
        }
        if (decimals != null && integer.isEmpty()) {
            integer = "0";
            keptBeforeCaret++;
        }

        var cleaned = decimals == null ? integer : integer + "," + decimals;
        keptBeforeCaret = Math.min(keptBeforeCaret, cleaned.length());
        var text = decimals == null ? group(integer) : group(integer) + "," + decimals;

        int newCaret = 0;
        int seen = 0;
        while (newCaret < text.length() && seen < keptBeforeCaret) {
            if (text.charAt(newCaret) != '.') {
                seen++;
            }
            newCaret++;
        }
        return new Edit(text, newCaret);
    }

    private static String group(String digits) {
        var out = new StringBuilder();
        int first = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - first) % 3 == 0) {
                out.append('.');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}
