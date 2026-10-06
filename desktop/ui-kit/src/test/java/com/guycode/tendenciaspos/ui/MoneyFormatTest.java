package com.guycode.tendenciaspos.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyFormatTest {

    @Test
    void formatsWithThousandsSeparatorAndOptionalCents() {
        assertThat(MoneyFormat.format(new BigDecimal("1234567"))).isEqualTo("1.234.567");
        assertThat(MoneyFormat.format(new BigDecimal("1234.5"))).isEqualTo("1.234,50");
        assertThat(MoneyFormat.format(new BigDecimal("999"))).isEqualTo("999");
        assertThat(MoneyFormat.format(new BigDecimal("-15000"))).isEqualTo("-15.000");
        assertThat(MoneyFormat.format(null)).isEmpty();
    }

    @Test
    void parsesToScaleTwo() {
        assertThat(MoneyFormat.parse("1.234.567")).isEqualTo(new BigDecimal("1234567.00"));
        assertThat(MoneyFormat.parse("1.234,5")).isEqualTo(new BigDecimal("1234.50"));
        assertThat(MoneyFormat.parse(",75")).isEqualTo(new BigDecimal("0.75"));
        assertThat(MoneyFormat.parse("")).isNull();
        assertThat(MoneyFormat.parse("abc")).isNull();
    }

    @Test
    void normalizeGroupsDigitsAndKeepsCaretAfterTypedDigit() {
        var edit = MoneyFormat.normalize("12345", 5);
        assertThat(edit.text()).isEqualTo("12.345");
        assertThat(edit.caret()).isEqualTo(6);

        // Insertar "9" al inicio de "12.345".
        edit = MoneyFormat.normalize("912.345", 1);
        assertThat(edit.text()).isEqualTo("912.345");
        assertThat(edit.caret()).isEqualTo(1);

        edit = MoneyFormat.normalize("1.2345", 6);
        assertThat(edit.text()).isEqualTo("12.345");
        assertThat(edit.caret()).isEqualTo(6);
    }

    @Test
    void normalizeRejectsInvalidCharactersAndLimitsDigits() {
        assertThat(MoneyFormat.normalize("12a3", 4).text()).isEqualTo("123");
        assertThat(MoneyFormat.normalize("1,2,3", 5).text()).isEqualTo("1,23");
        assertThat(MoneyFormat.normalize("10,999", 6).text()).isEqualTo("10,99");
        assertThat(MoneyFormat.normalize("0005", 4).text()).isEqualTo("5");
        assertThat(MoneyFormat.normalize("0", 1).text()).isEqualTo("0");
        assertThat(MoneyFormat.normalize(",5", 2)).isEqualTo(new MoneyFormat.Edit("0,5", 3));
        assertThat(MoneyFormat.normalize("1234567890123", 13).text()).isEqualTo("123.456.789.012");
    }
}
