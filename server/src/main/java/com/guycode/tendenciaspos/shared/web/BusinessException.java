package com.guycode.tendenciaspos.shared.web;

import java.io.Serial;

/** Regla de negocio incumplida; se responde como Problem Details con su código. */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ErrorCode code() {
        return code;
    }
}
