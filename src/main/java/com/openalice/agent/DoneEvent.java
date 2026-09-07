package com.openalice.agent;

public record DoneEvent(String reply) implements AgentEvent {
    public DoneEvent {
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("reply must not be blank");
        }
        reply = reply.trim();
    }
}
