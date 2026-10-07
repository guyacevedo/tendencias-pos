package com.guycode.tendenciaspos.desktop.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DatesTest {
    private static final Instant INSTANT = Instant.parse("2026-10-06T19:35:00Z");

    @Test
    void muestraLaHoraDeColombia() {
        assertThat(Dates.time(INSTANT)).isEqualTo("14:35");
        assertThat(Dates.dateTime(INSTANT)).isEqualTo("06/10/2026 14:35");
    }

    @Test
    void sinValorNoMuestraNada() {
        assertThat(Dates.time(null)).isEmpty();
        assertThat(Dates.dateTime(null)).isEmpty();
    }

    @Test
    void leeLaHoraDeUnInstanteEnTextoYAguantaBasura() {
        assertThat(Dates.timeFromIso("2026-10-06T19:35:00Z")).isEqualTo("14:35");
        assertThat(Dates.timeFromIso("mañana")).isNull();
        assertThat(Dates.timeFromIso(null)).isNull();
    }
}
