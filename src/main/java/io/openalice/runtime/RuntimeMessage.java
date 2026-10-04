package io.openalice.runtime;

public record RuntimeMessage(Role role, String text) {

    public RuntimeMessage {
        if (role == null || text == null || text.isBlank()) {
            throw new IllegalArgumentException("Runtime message requires a role and text");
        }
    }

    public enum Role {
        USER,
        ASSISTANT
    }
}
