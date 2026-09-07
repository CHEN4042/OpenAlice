package openalice.memory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import openalice.model.ChatMessage;
import openalice.model.SessionId;
import openalice.model.UserId;
import openalice.port.MemoryPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryMemoryPortTest {

    @Test
    void shouldKeepSessionsIsolated() {
        MemoryPort memory = new InMemoryMemoryPort();
        var userId = UserId.of("user");
        memory.append(ChatMessage.user(userId, SessionId.of("a"), "Alpha"));
        memory.append(ChatMessage.user(userId, SessionId.of("b"), "Beta"));

        assertThat(memory.history(userId, SessionId.of("a"), 10))
                .extracting(ChatMessage::content)
                .containsExactly("Alpha");
        assertThat(memory.history(userId, SessionId.of("b"), 10))
                .extracting(ChatMessage::content)
                .containsExactly("Beta");
    }

    @Test
    void shouldReturnLimitedTailAndImmutableCopy() {
        MemoryPort memory = new InMemoryMemoryPort();
        var userId = UserId.of("user");
        var sessionId = SessionId.of("session");
        memory.append(ChatMessage.user(userId, sessionId, "1"));
        memory.append(ChatMessage.user(userId, sessionId, "2"));
        memory.append(ChatMessage.user(userId, sessionId, "3"));

        List<ChatMessage> history = memory.history(userId, sessionId, 2);
        assertThat(history).extracting(ChatMessage::content).containsExactly("2", "3");

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> history.add(ChatMessage.user(userId, sessionId, "4"))
        );
    }

    @Test
    void shouldAppendConcurrentlyToSameSession() throws Exception {
        MemoryPort memory = new InMemoryMemoryPort();
        var userId = UserId.of("user");
        var sessionId = SessionId.of("session");
        List<Callable<Void>> tasks = IntStream.range(0, 128)
                .mapToObj(index -> (Callable<Void>) () -> {
                    memory.append(ChatMessage.user(userId, sessionId, "message-" + index));
                    return null;
                })
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            executor.invokeAll(tasks);
        } finally {
            executor.shutdownNow();
        }

        assertThat(memory.history(userId, sessionId, 128)).hasSize(128);
    }

    @Test
    void shouldRejectNullMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryMemoryPort().append(null));
    }
}

