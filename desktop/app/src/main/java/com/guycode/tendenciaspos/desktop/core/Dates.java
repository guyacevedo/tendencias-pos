package com.guycode.tendenciaspos.desktop.core;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Fechas y horas para mostrar: el servidor habla en UTC, el usuario lee en hora de Colombia. */
public final class Dates {
    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Dates() {}

    public static String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant.atZone(ZONE));
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(ZONE));
    }

    /** Hora de un instante en texto ISO (como viene en un Problem Details), o {@code null} si no se entiende. */
    public static String timeFromIso(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return null;
        }
        try {
            return time(Instant.parse(isoInstant));
        } catch (DateTimeException e) {
            return null;
        }
    }
}
