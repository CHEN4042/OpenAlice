package io.openalice.exception;

import java.util.UUID;

public class ExecutionNotFoundException extends RuntimeException {
    public ExecutionNotFoundException(UUID executionId) {
        super("Execution event stream not found: " + executionId);
    }
}
