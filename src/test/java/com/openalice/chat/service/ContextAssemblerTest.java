package com.openalice.chat.service;

import com.openalice.agent.AgentRequest;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.chat.store.memory.InMemoryConversationStore;
import com.openalice.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ContextAssemblerTest {

    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session";
    private static final String SYSTEM_PROMPT = "You are Alice, a warm and attentive AI companion.";

    @Test
    void shouldReadRecentHistoryFromConversationStore() {
        ConversationStore store = new InMemoryConversationStore();
        for (int index = 1; index <= 6; index++) {
            store.append(StoredMessage.user(USER_ID, SESSION_ID, "message-" + index));
        }
        ContextAssembler assembler = new ContextAssembler(
                store,
                SYSTEM_PROMPT,
                3
        );

        AgentRequest request = assembler.assemble(USER_ID, SESSION_ID);

        assertThat(request.userId()).isEqualTo(USER_ID);
        assertThat(request.sessionId()).isEqualTo(SESSION_ID);
        assertThat(request.systemPrompt()).isEqualTo(SYSTEM_PROMPT);
        assertThat(request.conversationContext())
                .extracting(ChatMessage::content)
                .containsExactly("message-4", "message-5", "message-6");
    }

    @Test
    void shouldRejectContextThatDoesNotEndWithUserMessage() {
        ConversationStore store = new InMemoryConversationStore();
        store.append(StoredMessage.assistant(USER_ID, SESSION_ID, "assistant without user"));
        ContextAssembler assembler = new ContextAssembler(
                store,
                SYSTEM_PROMPT,
                20
        );

        assertThatIllegalStateException()
                .isThrownBy(() -> assembler.assemble(USER_ID, SESSION_ID));
    }
}
