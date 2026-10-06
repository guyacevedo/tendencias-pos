package com.guycode.tendenciaspos.contracts;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ApiVersionTest {
    @Test
    void rechazaValoresNulos() {
        assertThatNullPointerException().isThrownBy(() -> new ApiVersion(null, "1.0.0"));
    }
}
