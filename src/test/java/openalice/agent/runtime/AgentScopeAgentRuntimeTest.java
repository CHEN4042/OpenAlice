package openalice.agent.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import openalice.model.ChatMessage;
import openalice.model.SessionId;
import openalice.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentScopeAgentRuntimeTest {

    @Test
    void shouldReplyViaDeterministicModelByDefault() {
        try (AgentRuntime runtime = AgentRuntimeFactory.create()) {
            ChatResult result = runtime.chat(ChatMessage.user(
                    UserId.of("user"), SessionId.of("session"), "你好"
            ));

            assertThat(result.reply()).isEqualTo("收到：你好");
        }
    }

    @Test
    void shouldHandleConcurrentSessions() throws Exception {
        try (AgentRuntime runtime = AgentRuntimeFactory.create()) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<String> alpha = executor.submit(() -> runtime.chat(
                        ChatMessage.user(UserId.of("user"), SessionId.of("alpha"), "Alpha")
                ).reply());
                Future<String> beta = executor.submit(() -> runtime.chat(
                        ChatMessage.user(UserId.of("user"), SessionId.of("beta"), "Beta")
                ).reply());
                assertThat(alpha.get(10, TimeUnit.SECONDS)).isEqualTo("收到：Alpha");
                assertThat(beta.get(10, TimeUnit.SECONDS)).isEqualTo("收到：Beta");
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
