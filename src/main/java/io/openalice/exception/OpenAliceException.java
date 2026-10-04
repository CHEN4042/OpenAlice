package io.openalice.exception;

import java.util.Objects;

public class OpenAliceException extends RuntimeException {

    private final ErrorCode errorCode;

    public OpenAliceException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public OpenAliceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public OpenAliceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
