package com.openalice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ConversationTurnTest {

    @Test
    void shouldFollowReceivedRunningCompletedLifecycle() {
        ChatMessage userMessage = ChatMessage.user(UserId.DEFAULT, SessionId.of("session"), "hi");
        ConversationTurn turn = ConversationTurn.received(userMessage);

        assertThat(turn.status()).isEqualTo(TurnStatus.RECEIVED);
        assertThat(turn.userMessageId()).isEqualTo(userMessage.id());

        ConversationTurn running = turn.running();
        ConversationTurn completed = running.completed("assistant-message");

        assertThat(running.status()).isEqualTo(TurnStatus.RUNNING);
        assertThat(running.startedAt()).isNotNull();
        assertThat(completed.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(completed.assistantMessageId()).isEqualTo("assistant-message");
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void shouldRecordFailureAndRejectInvalidTransition() {
        ChatMessage userMessage = ChatMessage.user(UserId.DEFAULT, SessionId.of("session"), "hi");
        ConversationTurn failed = ConversationTurn.received(userMessage).running().failed("provider unavailable");

        assertThat(failed.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(failed.error()).isEqualTo("provider unavailable");

        assertThatIllegalStateException().isThrownBy(() -> failed.running());
    }
}
