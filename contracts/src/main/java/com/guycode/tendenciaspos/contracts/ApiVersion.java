package com.guycode.tendenciaspos.contracts;

import java.util.Objects;

/**
 * Versión publicada por la API en {@code GET /api/version}.
 *
 * @param serverVersion versión del servidor desplegado
 * @param minClientVersion versión mínima del escritorio que el servidor acepta
 */
public record ApiVersion(String serverVersion, String minClientVersion) {
    public ApiVersion {
        Objects.requireNonNull(serverVersion, "serverVersion");
        Objects.requireNonNull(minClientVersion, "minClientVersion");
    }
}
