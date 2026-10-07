package com.guycode.tendenciaspos.shared.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Configuración de la tienda contra PostgreSQL: la fila que crea la migración y su actualización. */
@Tag("it")
@Testcontainers
@SpringBootTest(properties = "tpos.identity.admin-initial-password=inicial-del-it")
@AutoConfigureMockMvc
class SettingsFlowIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcTemplate jdbc;

    /** El admin inicial solo cambia la clave una vez; el token vale para todas las pruebas de la clase. */
    private static String adminAuth;

    @BeforeEach
    void authenticate() {
        if (adminAuth == null) {
            var login = post("/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"inicial-del-it\"}");
            var changed = post(
                    "/api/auth/password",
                    "Bearer " + login.at("/accessToken").asString(),
                    "{\"currentPassword\":\"inicial-del-it\",\"newPassword\":\"clave-definitiva-1\"}");
            adminAuth = "Bearer " + changed.at("/accessToken").asString();
        }
    }

    @Test
    void laMigracionDejaLaTiendaListaYElAdminLaActualiza() {
        var admin = adminAuth;

        var initial =
                mvc.get().uri("/api/settings").header("Authorization", admin).exchange();
        assertThat(initial).hasStatusOk();
        assertThat(initial).bodyJson().extractingPath("$.storeName").isEqualTo("Tendencias Shoes");
        assertThat(initial).bodyJson().extractingPath("$.invoiceFormat").isEqualTo("RECEIPT_80MM");

        var saved = mvc.put()
                .uri("/api/settings")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"Tendencias Shoes SAS\",\"taxId\":\"900123456-7\","
                        + "\"address\":\"Calle 10 # 5-20\",\"phone\":\"3001234567\",\"invoiceFormat\":\"LETTER\"}")
                .exchange();
        assertThat(saved).hasStatusOk();
        assertThat(saved).bodyJson().extractingPath("$.storeName").isEqualTo("Tendencias Shoes SAS");

        assertThat(jdbc.queryForObject("select invoice_format from store_settings", String.class))
                .isEqualTo("LETTER");
        assertThat(jdbc.queryForList("select action from audit_log order by id", String.class))
                .contains("SETTINGS_UPDATED");
    }

    @Test
    void elNombreVacioRespondeValidacion() {
        var result = mvc.put()
                .uri("/api/settings")
                .header("Authorization", adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"  \",\"invoiceFormat\":\"LETTER\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors.storeName")
                .asString()
                .isNotEmpty();
    }

    private JsonNode post(String uri, String authorization, String body) {
        var request =
                mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        if (authorization != null) {
            request = request.header("Authorization", authorization);
        }
        var result = request.exchange();
        assertThat(result).hasStatusOk();
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
