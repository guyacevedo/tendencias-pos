package com.guycode.tendenciaspos.identity.adapter.in.web;

import static com.guycode.tendenciaspos.support.TestTokens.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.guycode.tendenciaspos.identity.application.Actor;
import com.guycode.tendenciaspos.identity.application.AuthService;
import com.guycode.tendenciaspos.identity.application.IssuedSession;
import com.guycode.tendenciaspos.identity.application.LoginResult;
import com.guycode.tendenciaspos.identity.application.RefreshResult;
import com.guycode.tendenciaspos.identity.application.UserAdminService;
import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.support.ApiWebTest;
import com.guycode.tendenciaspos.support.TestTokens;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Contrato HTTP y seguridad de {@code /api/auth} y {@code /api/users}. */
@ApiWebTest
@WebMvcTest(controllers = {AuthController.class, UserController.class})
class IdentityWebTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TestTokens tokens;

    @MockitoBean
    AuthService auth;

    @MockitoBean
    UserAdminService users;

    private static User ana() {
        return User.restore(7, "ana", "Ana", "hash", true, 0, null, false, Set.of(Role.CASHIER), NOW, NOW);
    }

    @Test
    void loginCorrectoDevuelveTokensYUsuario() {
        given(auth.login(eq("ana"), eq("clave-segura-1"), anyString()))
                .willReturn(new LoginResult.Success(new IssuedSession("jwt", NOW, "refresh", NOW, ana())));

        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"clave-segura-1\"}")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.accessToken").isEqualTo("jwt");
        assertThat(result).bodyJson().extractingPath("$.refreshToken").isEqualTo("refresh");
        assertThat(result).bodyJson().extractingPath("$.user.roles[0]").isEqualTo("CASHIER");
        assertThat(result).bodyJson().extractingPath("$.accessTokenExpiresAt").isEqualTo("2026-10-06T12:00:00Z");
    }

    @Test
    void loginIncorrectoRespondeCredencialesInvalidas() {
        given(auth.login(any(), any(), anyString())).willReturn(new LoginResult.InvalidCredentials());

        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"x\"}")
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void cuentaBloqueadaInformaHastaCuando() {
        given(auth.login(any(), any(), anyString())).willReturn(new LoginResult.Locked(NOW));

        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"x\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.LOCKED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
        assertThat(result).bodyJson().extractingPath("$.lockedUntil").isEqualTo("2026-10-06T12:00:00Z");
    }

    @Test
    void refreshRechazadoResponde401() {
        given(auth.refresh(any(), anyString())).willReturn(new RefreshResult.Rejected());

        var result = mvc.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"usado\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutNoExigeAccessToken() {
        assertThat(mvc.post()
                        .uri("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"r\"}"))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void sinTokenResponde401ProblemDetails() {
        var result = mvc.get().uri("/api/users").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void tokenVencidoResponde401TokenExpired() {
        var result = mvc.get()
                .uri("/api/auth/me")
                .header("Authorization", bearer(tokens.expired()))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void tokenConFirmaAjenaResponde401() {
        var forged = tokens.admin().substring(0, tokens.admin().lastIndexOf('.') + 1) + "firmaFalsa";

        var result = mvc.get()
                .uri("/api/users")
                .header("Authorization", bearer(forged))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void cajeroNoAccedeAUsuarios() {
        var result = mvc.get()
                .uri("/api/users")
                .header("Authorization", bearer(tokens.cashier()))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void administradorListaUsuarios() {
        given(users.list()).willReturn(List.of(ana()));

        var result = mvc.get()
                .uri("/api/users")
                .header("Authorization", bearer(tokens.admin()))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].username").isEqualTo("ana");
        assertThat(result).bodyJson().extractingPath("$[0].lockedUntil").isNull();
    }

    @Test
    void administradorCreaUsuarioConLocation() {
        given(users.create(any(Actor.class), eq("ana"), eq("Ana"), eq("temporal-123"), eq(Set.of(Role.CASHIER))))
                .willReturn(ana());

        var result = mvc.post()
                .uri("/api/users")
                .header("Authorization", bearer(tokens.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"username\":\"ana\",\"fullName\":\"Ana\",\"password\":\"temporal-123\",\"roles\":[\"CASHIER\"]}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED).hasHeader(HttpHeaders.LOCATION, "/api/users/7");
    }

    @Test
    void conCambioDeClavePendienteSoloSePermiteLaCuenta() {
        var token = bearer(tokens.adminMustChangePassword());
        given(auth.currentUser(any())).willReturn(ana());

        var blocked = mvc.get().uri("/api/users").header("Authorization", token).exchange();
        assertThat(blocked).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(blocked).bodyJson().extractingPath("$.code").isEqualTo("PASSWORD_CHANGE_REQUIRED");

        assertThat(mvc.get().uri("/api/auth/me").header("Authorization", token)).hasStatusOk();
    }
}
