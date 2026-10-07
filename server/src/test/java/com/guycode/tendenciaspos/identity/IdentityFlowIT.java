package com.guycode.tendenciaspos.identity;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Flujo completo contra PostgreSQL: admin inicial → cambio de clave → cajero con menos permisos. */
@Tag("it")
@Testcontainers
@SpringBootTest(properties = "tpos.identity.admin-initial-password=inicial-del-it")
@AutoConfigureMockMvc
class IdentityFlowIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void adminInicialCambiaClaveCreaCajeroYElCajeroNoAdministraUsuarios() {
        var first = login("admin", "inicial-del-it");
        assertThat(first.at("/user/passwordChangeRequired").asBoolean()).isTrue();
        assertThat(mvc.get().uri("/api/users").header("Authorization", bearer(first)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PASSWORD_CHANGE_REQUIRED");

        var changed = post(
                "/api/auth/password",
                bearer(first),
                "{\"currentPassword\":\"inicial-del-it\",\"newPassword\":\"clave-definitiva-1\"}");
        assertThat(changed.at("/user/passwordChangeRequired").asBoolean()).isFalse();

        var created = mvc.post()
                .uri("/api/users")
                .header("Authorization", bearer(changed))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Cajero1\",\"fullName\":\"Luis\",\"password\":\"temporal-123\","
                        + "\"roles\":[\"CASHIER\"]}")
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);

        var cashier = login("cajero1", "temporal-123");
        assertThat(cashier.at("/user/roles/0").asString()).isEqualTo("CASHIER");
        var cashierReady = post(
                "/api/auth/password",
                bearer(cashier),
                "{\"currentPassword\":\"temporal-123\",\"newPassword\":\"clave-cajero-1\"}");
        assertThat(mvc.get().uri("/api/users").header("Authorization", bearer(cashierReady)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("FORBIDDEN");

        var renewed = post(
                "/api/auth/refresh",
                null,
                "{\"refreshToken\":\"" + changed.at("/refreshToken").asString() + "\"}");
        assertThat(renewed.at("/refreshToken").asString())
                .isNotEqualTo(changed.at("/refreshToken").asString());
        assertThat(mvc.get().uri("/api/users").header("Authorization", bearer(renewed)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()")
                .isEqualTo(2);

        assertThat(jdbc.queryForList("select action from audit_log order by id", String.class))
                .contains("USER_CREATED", "LOGIN_SUCCESS", "PASSWORD_CHANGED");
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuenta() {
        for (int i = 0; i < 4; i++) {
            assertThat(loginRaw("admin", "mala-clave-" + i)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
        assertThat(loginRaw("admin", "mala-clave-5"))
                .hasStatus(HttpStatus.LOCKED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ACCOUNT_LOCKED");
        jdbc.update("update app_user set locked_until = null, failed_attempts = 0 where username = 'admin'");
    }

    private MvcTestResult loginRaw(String user, String pass) {
        return mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                .exchange();
    }

    private JsonNode login(String user, String pass) {
        var result = loginRaw(user, pass);
        assertThat(result).hasStatusOk();
        return json.readTree(result.getResponse().getContentAsByteArray());
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

    private static String bearer(JsonNode tokens) {
        return "Bearer " + tokens.at("/accessToken").asString();
    }
}
