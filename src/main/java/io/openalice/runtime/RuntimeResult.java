package io.openalice.runtime;

public record RuntimeResult(String text) {
    public RuntimeResult {
        text = text == null ? "" : text;
    }
}
