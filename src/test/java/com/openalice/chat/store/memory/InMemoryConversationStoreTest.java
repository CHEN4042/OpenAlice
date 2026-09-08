package com.openalice.chat.store.memory;

import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryConversationStoreTest {

    private static final String USER_ID = "user-1";

    @Test
    void shouldKeepSessionsIsolated() {
        ConversationStore store = new InMemoryConversationStore();
        store.append(StoredMessage.user(USER_ID, "a", "Alpha"));
        store.append(StoredMessage.user(USER_ID, "b", "Beta"));

        assertThat(store.history("a", 10))
                .extracting(StoredMessage::content)
                .containsExactly("Alpha");
        assertThat(store.history("b", 10))
                .extracting(StoredMessage::content)
                .containsExactly("Beta");
    }

    @Test
    void shouldReturnLimitedTailAndImmutableCopy() {
        ConversationStore store = new InMemoryConversationStore();
        String sessionId = "session";
        store.append(StoredMessage.user(USER_ID, sessionId, "1"));
        store.append(StoredMessage.user(USER_ID, sessionId, "2"));
        store.append(StoredMessage.user(USER_ID, sessionId, "3"));

        List<StoredMessage> history = store.history(sessionId, 2);
        assertThat(history).extracting(StoredMessage::content).containsExactly("2", "3");

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> history.add(StoredMessage.user(USER_ID, sessionId, "4"))
        );
    }

    @Test
    void shouldAppendConcurrentlyToSameSession() throws Exception {
        ConversationStore store = new InMemoryConversationStore();
        String sessionId = "session";
        List<Callable<Void>> tasks = IntStream.range(0, 128)
                .mapToObj(index -> (Callable<Void>) () -> {
                    store.append(StoredMessage.user(USER_ID, sessionId, "message-" + index));
                    return null;
                })
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            executor.invokeAll(tasks);
        } finally {
            executor.shutdownNow();
        }

        assertThat(store.history(sessionId, 128)).hasSize(128);
    }

    @Test
    void shouldRejectNullMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryConversationStore().append(null));
    }
}
