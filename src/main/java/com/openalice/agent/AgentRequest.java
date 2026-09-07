package com.openalice.agent;

import com.openalice.model.ChatMessage;
import com.openalice.model.ConversationTurn;
import java.util.List;
import java.util.Objects;

/**
 * Explicit input to the agent runtime.
 *
 * <p>conversationContext contains the recent business history, including the
 * current USER message. systemPrompt is assembled separately by the service
 * layer so the agent adapter does not read the store itself.</p>
 */
public record AgentRequest(
        ConversationTurn turn,
        List<ChatMessage> conversationContext,
        String systemPrompt
) {

    public AgentRequest {
        Objects.requireNonNull(turn, "turn must not be null");
        conversationContext = List.copyOf(Objects.requireNonNull(
                conversationContext,
                "conversationContext must not be null"
        ));
        systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt must not be null").trim();
        if (systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
    }
}
