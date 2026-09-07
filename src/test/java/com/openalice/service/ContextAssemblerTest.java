package com.openalice.service;

import com.openalice.agent.AgentRequest;
import com.openalice.agent.runtime.AgentRuntimeProperties;
import com.openalice.model.ChatMessage;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import com.openalice.repository.ConversationStore;
import com.openalice.repository.memory.InMemoryConversationStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAssemblerTest {

    @Test
    void shouldReadRecentHistoryFromConversationStore() {
        ConversationStore store = new InMemoryConversationStore();
        SessionId sessionId = SessionId.of("session");
        for (int index = 1; index <= 5; index++) {
            store.append(ChatMessage.user(UserId.DEFAULT, sessionId, "message-" + index));
        }
        ChatMessage currentUserMessage = ChatMessage.user(UserId.DEFAULT, sessionId, "message-6");
        store.append(currentUserMessage);
        ContextAssembler assembler = new ContextAssembler(
                store,
                AgentRuntimeProperties.defaults(),
                3
        );

        AgentRequest request = assembler.assemble(
                com.openalice.model.ConversationTurn.received(currentUserMessage),
                currentUserMessage
        );

        assertThat(request.systemPrompt()).isEqualTo(AgentRuntimeProperties.defaults().systemPrompt());
        assertThat(request.conversationContext())
                .extracting(ChatMessage::content)
                .containsExactly("message-4", "message-5", "message-6");
    }
}
