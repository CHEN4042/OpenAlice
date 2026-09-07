package com.openalice.agent;

public record ErrorEvent(String error) implements AgentEvent {
    public ErrorEvent {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }
        error = error.trim();
    }
}
