package com.openalice.service;

import com.openalice.agent.AgentRequest;
import com.openalice.agent.runtime.AgentRuntimeProperties;
import com.openalice.model.ChatMessage;
import com.openalice.model.ConversationTurn;
import com.openalice.repository.ConversationStore;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds the explicit agent input from the conversation store.
 *
 * <p>This keeps history assembly out of the controller and out of the AgentScope
 * adapter. The store remains the source of truth for business history.</p>
 */
@Service
public class ContextAssembler {

    private final ConversationStore conversationStore;
    private final String systemPrompt;
    private final int contextWindowSize;

    public ContextAssembler(
            ConversationStore conversationStore,
            AgentRuntimeProperties properties,
            @Value("${openalice.agent.context-window-size:20}") int contextWindowSize
    ) {
        this.conversationStore = conversationStore;
        this.systemPrompt = properties.systemPrompt();
        this.contextWindowSize = contextWindowSize;
        if (contextWindowSize <= 0) {
            throw new IllegalArgumentException("contextWindowSize must be positive");
        }
    }

    public AgentRequest assemble(ConversationTurn turn, ChatMessage userMessage) {
        if (turn == null || userMessage == null || !turn.userMessageId().equals(userMessage.id())) {
            throw new IllegalArgumentException("turn and userMessage must describe the same turn");
        }
        List<ChatMessage> context = conversationStore.history(
                turn.userId(),
                turn.sessionId(),
                contextWindowSize
        );
        if (context.stream().noneMatch(message -> message.id().equals(userMessage.id()))) {
            throw new IllegalStateException("current USER message is not present in conversation context");
        }
        return new AgentRequest(turn, context, systemPrompt);
    }
}
