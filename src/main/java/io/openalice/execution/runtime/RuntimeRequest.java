package io.openalice.execution.runtime;

import java.util.List;
import java.util.UUID;

public record RuntimeRequest(
        UUID executionId, String userId, String systemPrompt, List<RuntimeMessage> messages) {

    public RuntimeRequest {
        if (executionId == null || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Runtime request requires execution and user identity");
        }
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        messages = List.copyOf(messages);
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Runtime request requires at least one message");
        }
    }
}
