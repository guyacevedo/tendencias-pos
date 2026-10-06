package com.guycode.tendenciaspos.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.shared.config.TposProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(TposProperties.class)
@WebMvcTest(controllers = VersionController.class, properties = "tpos.min-client-version=1.2.3")
class VersionControllerTest {
    @Autowired
    MockMvcTester mvc;

    @Test
    void devuelveVersionDelServidorYMinimaDelCliente() {
        assertThat(mvc.get().uri("/api/version"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.minClientVersion")
                .isEqualTo("1.2.3");
    }

    @Test
    void rutaInexistenteRespondeProblemDetailsConCodigo() {
        var result = mvc.get().uri("/api/no-existe").exchange();
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void metodoNoPermitidoRespondeProblemDetails() {
        assertThat(mvc.post().uri("/api/version"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("METHOD_NOT_ALLOWED");
    }
}
