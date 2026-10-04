package io.openalice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_ARGUMENT("invalid_argument", "Invalid argument", HttpStatus.BAD_REQUEST),
    NOT_FOUND("not_found", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("conflict", "Resource conflict", HttpStatus.CONFLICT),
    INVALID_STATE("invalid_state", "Invalid state", HttpStatus.CONFLICT),
    RUNTIME_ERROR("runtime_error", "Runtime execution failed", HttpStatus.BAD_GATEWAY),
    DATABASE_ERROR("database_error", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    CONFIG_ERROR("config_error", "Application configuration is invalid", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("internal_error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
