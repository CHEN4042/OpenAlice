package com.openalice.agent;

/** 错误事件：携带给到调用方的可读错误信息。 */
public record ErrorEvent(String error) implements AgentEvent {
    public ErrorEvent {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }
        error = error.trim();
    }
}
