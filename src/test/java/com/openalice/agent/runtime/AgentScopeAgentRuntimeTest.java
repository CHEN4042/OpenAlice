package com.openalice.agent.runtime;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.model.ChatMessage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentScopeAgentRuntimeTest {

    private static final String USER_ID = "user-1";

    @Test
    void shouldStreamAgentEventsViaDeterministicModelByDefault() {
        try (AgentRuntime runtime = AgentRuntimeFactory.create(AgentRuntimePropertiesFixture.mock())) {
            List<AgentEvent> events = runtime.stream(request("stream", "你好"))
                    .collectList()
                    .block();

            assertThat(events).extracting(event -> event.getClass().getSimpleName())
                    .contains("TextDeltaEvent", "DoneEvent");
            assertThat(events.stream().filter(TextDeltaEvent.class::isInstance)
                    .map(TextDeltaEvent.class::cast)
                    .map(TextDeltaEvent::delta)
                    .reduce("", String::concat))
                    .isEqualTo("收到：你好");
            assertThat(events.stream().filter(DoneEvent.class::isInstance)
                    .map(DoneEvent.class::cast)
                    .map(DoneEvent::reply)
                    .findFirst())
                    .contains("收到：你好");
        }
    }

    @Test
    void shouldKeepSessionsIsolatedUnderConcurrency() throws Exception {
        try (AgentRuntime runtime = AgentRuntimeFactory.create(AgentRuntimePropertiesFixture.mock())) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<List<AgentEvent>> alpha = executor.submit(
                        () -> runtime.stream(request("alpha", "Alpha")).collectList().block()
                );
                Future<List<AgentEvent>> beta = executor.submit(
                        () -> runtime.stream(request("beta", "Beta")).collectList().block()
                );
                assertThat(replyOf(alpha.get(10, TimeUnit.SECONDS))).isEqualTo("收到：Alpha");
                assertThat(replyOf(beta.get(10, TimeUnit.SECONDS))).isEqualTo("收到：Beta");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static String replyOf(List<AgentEvent> events) {
        return events.stream()
                .filter(DoneEvent.class::isInstance)
                .map(DoneEvent.class::cast)
                .map(DoneEvent::reply)
                .findFirst()
                .orElseThrow();
    }

    private static AgentRequest request(String sessionId, String message) {
        return new AgentRequest(
                USER_ID,
                sessionId,
                List.of(ChatMessage.user(message)),
                "test system prompt"
        );
    }
}
