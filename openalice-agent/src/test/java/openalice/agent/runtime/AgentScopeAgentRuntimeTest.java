package openalice.agent.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import openalice.core.domain.ChatMessage;
import openalice.core.domain.SessionId;
import openalice.core.domain.UserId;
import openalice.core.port.MemoryPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentScopeAgentRuntimeTest {

    private static final class RecordingMemoryPort implements MemoryPort {
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public synchronized void append(ChatMessage message) {
            messages.add(message);
        }

        @Override
        public synchronized List<ChatMessage> history(UserId userId, SessionId sessionId, int limit) {
            return messages.stream()
                    .filter(message -> message.userId().equals(userId) && message.sessionId().equals(sessionId))
                    .limit(limit)
                    .toList();
        }
    }

    @Test
    void shouldWriteUserAndAssistantMessages() {
        MemoryPort memory = new RecordingMemoryPort();
        try (AgentRuntime runtime = AgentRuntimeFactory.create(memory)) {
            ChatResult result = runtime.chat(ChatMessage.user(
                    UserId.of("user"), SessionId.of("session"), "你好"
            ));

            assertThat(result.reply()).isEqualTo("收到：你好");
            assertThat(memory.history(UserId.of("user"), SessionId.of("session"), 10))
                    .extracting(ChatMessage::role)
                    .containsExactly(openalice.core.domain.MessageRole.USER, openalice.core.domain.MessageRole.ASSISTANT);
        }
    }

    @Test
    void shouldIsolateConcurrentSessions() throws Exception {
        MemoryPort memory = new RecordingMemoryPort();
        try (AgentRuntime runtime = AgentRuntimeFactory.create(memory)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> first = executor.submit(() -> runtime.chat(
                        ChatMessage.user(UserId.of("user"), SessionId.of("alpha"), "Alpha")
                ));
                Future<?> second = executor.submit(() -> runtime.chat(
                        ChatMessage.user(UserId.of("user"), SessionId.of("beta"), "Beta")
                ));
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertThat(memory.history(UserId.of("user"), SessionId.of("alpha"), 10))
                    .allSatisfy(message -> assertThat(message.content()).contains("Alpha"));
            assertThat(memory.history(UserId.of("user"), SessionId.of("beta"), 10))
                    .allSatisfy(message -> assertThat(message.content()).contains("Beta"));
        }
    }
}

