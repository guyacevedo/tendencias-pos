package com.guycode.tendenciaspos.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Códigos de error estables que el escritorio traduce a mensajes para el usuario. */
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "La solicitud no es válida."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso no existe."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Operación no permitida sobre este recurso."),
    CONFLICT(HttpStatus.CONFLICT, "La operación entra en conflicto con el estado actual."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** Código genérico para un estado HTTP producido por el propio framework. */
    public static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
        };
    }
}
