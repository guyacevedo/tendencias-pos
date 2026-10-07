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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor."),

    // Identidad
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "La cuenta está bloqueada temporalmente por intentos fallidos."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "La sesión expiró. Vuelva a iniciar sesión."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida. Inicie sesión de nuevo."),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "La clave actual no es correcta."),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "Debe cambiar su clave antes de continuar."),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "La clave debe tener entre 10 y 128 caracteres."),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre."),
    LAST_ADMIN(HttpStatus.CONFLICT, "Debe quedar al menos un administrador activo.");

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
