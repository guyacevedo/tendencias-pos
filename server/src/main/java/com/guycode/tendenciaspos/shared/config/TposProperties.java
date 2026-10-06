package com.guycode.tendenciaspos.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de la aplicación (prefijo {@code tpos}).
 *
 * @param minClientVersion versión mínima del escritorio aceptada por la API
 */
@Validated
@ConfigurationProperties("tpos")
public record TposProperties(@NotBlank String minClientVersion) {}
