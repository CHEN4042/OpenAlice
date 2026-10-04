package io.openalice.exception;

import io.openalice.common.log.OpenAliceLog;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenAliceException.class)
    public ResponseEntity<ProblemDetail> handleOpenAliceException(OpenAliceException error) {
        ErrorCode code = error.errorCode();
        OpenAliceLog.event("request.failed")
                .message("Request failed with an expected application error")
                .field("errorCode", code.code())
                .field("httpStatus", code.httpStatus().value())
                .warn();
        return ResponseEntity.status(code.httpStatus()).body(problem(code, error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception error) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        OpenAliceLog.event("request.failed")
                .message("Request failed with an unexpected error")
                .field("errorCode", code.code())
                .field("httpStatus", code.httpStatus().value())
                .field("exceptionType", error.getClass().getName())
                .error(error);
        return ResponseEntity.status(code.httpStatus())
                .body(problem(code, code.defaultMessage()));
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.httpStatus(), detail);
        problem.setTitle(code.defaultMessage());
        problem.setProperty("code", code.code());
        return problem;
    }
}
