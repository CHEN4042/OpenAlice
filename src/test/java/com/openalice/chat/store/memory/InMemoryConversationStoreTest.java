package com.openalice.chat.store.memory;

import com.openalice.model.ChatMessage;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import com.openalice.chat.store.ConversationStore;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryConversationStoreTest {

    @Test
    void shouldKeepSessionsIsolated() {
        ConversationStore store = new InMemoryConversationStore();
        var userId = UserId.DEFAULT;
        store.append(ChatMessage.user(userId, SessionId.of("a"), "Alpha"));
        store.append(ChatMessage.user(userId, SessionId.of("b"), "Beta"));

        assertThat(store.history(userId, SessionId.of("a"), 10))
                .extracting(ChatMessage::content)
                .containsExactly("Alpha");
        assertThat(store.history(userId, SessionId.of("b"), 10))
                .extracting(ChatMessage::content)
                .containsExactly("Beta");
    }

    @Test
    void shouldReturnLimitedTailAndImmutableCopy() {
        ConversationStore store = new InMemoryConversationStore();
        var userId = UserId.DEFAULT;
        var sessionId = SessionId.of("session");
        store.append(ChatMessage.user(userId, sessionId, "1"));
        store.append(ChatMessage.user(userId, sessionId, "2"));
        store.append(ChatMessage.user(userId, sessionId, "3"));

        List<ChatMessage> history = store.history(userId, sessionId, 2);
        assertThat(history).extracting(ChatMessage::content).containsExactly("2", "3");

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> history.add(ChatMessage.user(userId, sessionId, "4"))
        );
    }

    @Test
    void shouldAppendConcurrentlyToSameSession() throws Exception {
        ConversationStore store = new InMemoryConversationStore();
        var userId = UserId.DEFAULT;
        var sessionId = SessionId.of("session");
        List<Callable<Void>> tasks = IntStream.range(0, 128)
                .mapToObj(index -> (Callable<Void>) () -> {
                    store.append(ChatMessage.user(userId, sessionId, "message-" + index));
                    return null;
                })
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            executor.invokeAll(tasks);
        } finally {
            executor.shutdownNow();
        }

        assertThat(store.history(userId, sessionId, 128)).hasSize(128);
    }

    @Test
    void shouldRejectNullMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryConversationStore().append(null));
    }
}
