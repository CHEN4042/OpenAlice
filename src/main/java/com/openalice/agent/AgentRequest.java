package com.openalice.agent;

import com.openalice.model.ChatMessage;
import java.util.List;
import java.util.Objects;

/**
 * Explicit input to the agent runtime.
 *
 * <p>{@code userId} / {@code sessionId} identify the RuntimeContext; the single-user
 * API always passes the service's default user. {@code conversationContext} contains
 * the recent business history, including the current USER message. {@code systemPrompt}
 * is assembled separately by the service layer so the agent adapter does not read the
 * store itself.</p>
 */
public record AgentRequest(
        String userId,
        String sessionId,
        List<ChatMessage> conversationContext,
        String systemPrompt
) {

    public AgentRequest {
        userId = requireNotBlank(userId, "userId");
        sessionId = requireNotBlank(sessionId, "sessionId");
        conversationContext = List.copyOf(Objects.requireNonNull(
                conversationContext,
                "conversationContext must not be null"
        ));
        systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt must not be null").trim();
        if (systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
