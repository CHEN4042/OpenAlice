package com.openalice.agent;

public record TextDeltaEvent(String delta) implements AgentEvent {
    public TextDeltaEvent {
        if (delta == null || delta.isEmpty()) {
            throw new IllegalArgumentException("delta must not be empty");
        }
    }
}
