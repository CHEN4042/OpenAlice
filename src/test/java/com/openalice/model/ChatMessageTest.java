package com.openalice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatMessageTest {

    @Test
    void shouldExposeRoleAndContent() {
        var userMessage = ChatMessage.user("hello");
        var assistantMessage = ChatMessage.assistant("hi");

        assertThat(userMessage.role()).isEqualTo(MessageRole.USER);
        assertThat(userMessage.content()).isEqualTo("hello");
        assertThat(assistantMessage.role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(assistantMessage.content()).isEqualTo("hi");
    }

    @Test
    void shouldRejectBlankContent() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ChatMessage.user(" "));
    }

    @Test
    void shouldRejectNullRole() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatMessage(null, "hello"));
    }
}
