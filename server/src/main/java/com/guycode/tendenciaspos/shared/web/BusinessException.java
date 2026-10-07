package com.guycode.tendenciaspos.shared.web;

import java.io.Serial;
import java.util.Map;

/** Regla de negocio incumplida; se responde como Problem Details con su código. */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient Map<String, Object> properties;

    public BusinessException(ErrorCode code, String message, Map<String, ?> properties) {
        super(message);
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    public BusinessException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public BusinessException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    /** Datos de validación con el mensaje de cada campo inválido. */
    public static BusinessException validation(Map<String, String> fieldErrors) {
        return new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage(),
                Map.of("errors", fieldErrors));
    }

    public ErrorCode code() {
        return code;
    }

    /** Propiedades adicionales del Problem Details (p. ej. {@code lockedUntil}). */
    public Map<String, Object> properties() {
        return properties;
    }
}
