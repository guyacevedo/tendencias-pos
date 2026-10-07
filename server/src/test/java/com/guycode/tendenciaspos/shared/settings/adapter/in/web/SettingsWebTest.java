package com.guycode.tendenciaspos.shared.settings.adapter.in.web;

import static com.guycode.tendenciaspos.support.TestTokens.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.shared.settings.application.StoreSettingsService;
import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;
import com.guycode.tendenciaspos.support.ApiWebTest;
import com.guycode.tendenciaspos.support.TestTokens;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Contrato HTTP y permisos de {@code /api/settings}. */
@ApiWebTest
@WebMvcTest(controllers = SettingsController.class)
class SettingsWebTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final String BODY = """
            {"storeName":"Tendencias Shoes","taxId":"900123456-7","address":"Calle 10 # 5-20",\
            "phone":"3001234567","invoiceFormat":"LETTER"}""";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TestTokens tokens;

    @MockitoBean
    StoreSettingsService service;

    private static StoreSettings settings() {
        return new StoreSettings(
                "Tendencias Shoes", "900123456-7", "Calle 10 # 5-20", "3001234567", InvoiceFormat.LETTER, NOW);
    }

    @Test
    void cualquierUsuarioAutenticadoLeeLaConfiguracion() {
        given(service.get()).willReturn(settings());

        var result = mvc.get()
                .uri("/api/settings")
                .header("Authorization", bearer(tokens.cashier()))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.storeName").isEqualTo("Tendencias Shoes");
        assertThat(result).bodyJson().extractingPath("$.invoiceFormat").isEqualTo("LETTER");
        assertThat(result).bodyJson().extractingPath("$.updatedAt").isEqualTo("2026-10-06T12:00:00Z");
    }

    @Test
    void sinTokenNoSePuedeLeer() {
        var result = mvc.get().uri("/api/settings").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void elCajeroNoPuedeGuardar() {
        var result = mvc.put()
                .uri("/api/settings")
                .header("Authorization", bearer(tokens.cashier()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void elAdministradorGuardaYQuedaRegistradoQuienLoHizo() {
        given(service.update(
                        eq("admin"),
                        eq("Tendencias Shoes"),
                        eq("900123456-7"),
                        eq("Calle 10 # 5-20"),
                        eq("3001234567"),
                        any(InvoiceFormat.class)))
                .willReturn(settings());

        var result = mvc.put()
                .uri("/api/settings")
                .header("Authorization", bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.taxId").isEqualTo("900123456-7");
    }
}
