package com.guycode.tendenciaspos.desktop.api;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error de una llamada a la API. {@link #code()} es el código estable de Problem Details del servidor
 * (p. ej. {@code STOCK_INSUFICIENTE}) o uno de los códigos locales de esta clase.
 */
public final class ApiException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** No se pudo conectar o se agotó el tiempo de espera. */
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    /** Hay red, pero el proxy responde que la API no está disponible (502, 503, 504). */
    public static final String SERVER_UNAVAILABLE = "SERVER_UNAVAILABLE";
    /** El servidor respondió algo que no se pudo leer. */
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";
    /** El hilo se interrumpió mientras esperaba la respuesta. */
    public static final String INTERRUPTED = "INTERRUPTED";

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    public ApiException(String code, String message, int status, Map<String, ?> details, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    public ApiException(String code, String message, int status, Throwable cause) {
        this(code, message, status, Map.of(), cause);
    }

    public ApiException(String code, String message, int status) {
        this(code, message, status, Map.of(), null);
    }

    public String code() {
        return code;
    }

    /** Estado HTTP, o 0 si no hubo respuesta. */
    public int status() {
        return status;
    }

    /** Propiedades extra del Problem Details (p. ej. {@code lockedUntil} o {@code errors}). */
    public Map<String, Object> details() {
        return details;
    }

    /** Valor de texto de una propiedad extra, o {@code null} si no vino. */
    public String detail(String key) {
        var value = details.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /** Mensajes por campo de una respuesta {@code VALIDATION_FAILED}; vacío si no los trae. */
    public Map<String, String> fieldErrors() {
        var result = new LinkedHashMap<String, String>();
        if (details.get("errors") instanceof Map<?, ?> errors) {
            errors.forEach((field, message) -> result.put(String.valueOf(field), String.valueOf(message)));
        }
        return result;
    }

    /** {@code true} si el problema es de conexión con el servidor y no de la operación pedida. */
    public boolean isConnectivity() {
        return NETWORK_ERROR.equals(code) || SERVER_UNAVAILABLE.equals(code);
    }
}
