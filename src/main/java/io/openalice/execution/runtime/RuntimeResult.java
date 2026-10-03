package io.openalice.execution.runtime;

public record RuntimeResult(String text) {
    public RuntimeResult {
        text = text == null ? "" : text;
    }
}
