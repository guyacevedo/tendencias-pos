package com.guycode.tendenciaspos.desktop.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionsTest {

    @Test
    void comparesNumericPartsIgnoringSuffixes() {
        assertThat(Versions.isOlder("0.1.0-SNAPSHOT", "0.1.0")).isFalse();
        assertThat(Versions.isOlder("0.1.0", "0.2.0")).isTrue();
        assertThat(Versions.isOlder("0.10.0", "0.9.5")).isFalse();
        assertThat(Versions.isOlder("1.0", "1.0.1")).isTrue();
        assertThat(Versions.isOlder("2.0.0", "1.9.9")).isFalse();
    }
}
