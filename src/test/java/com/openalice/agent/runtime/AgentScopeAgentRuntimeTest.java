package com.openalice.agent.runtime;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.model.ChatMessage;
import com.openalice.model.ConversationTurn;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentScopeAgentRuntimeTest {

    @Test
    void shouldStreamAgentEventsViaDeterministicModelByDefault() {
        try (AgentRuntime runtime = AgentRuntimeFactory.create()) {
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
    void shouldExposeBlockingChatConvenienceMethod() {
        try (AgentRuntime runtime = AgentRuntimeFactory.create()) {
            ChatResult result = runtime.chat(request("session", "你好"));

            assertThat(result.userId()).isEqualTo(UserId.DEFAULT);
            assertThat(result.sessionId()).isEqualTo(SessionId.of("session"));
            assertThat(result.reply()).isEqualTo("收到：你好");
        }
    }

    @Test
    void shouldHandleConcurrentSessions() throws Exception {
        try (AgentRuntime runtime = AgentRuntimeFactory.create()) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<ChatResult> alpha = executor.submit(() -> runtime.chat(request("alpha", "Alpha")));
                Future<ChatResult> beta = executor.submit(() -> runtime.chat(request("beta", "Beta")));
                assertThat(alpha.get(10, TimeUnit.SECONDS).reply()).isEqualTo("收到：Alpha");
                assertThat(beta.get(10, TimeUnit.SECONDS).reply()).isEqualTo("收到：Beta");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static AgentRequest request(String sessionId, String message) {
        ChatMessage userMessage = ChatMessage.user(
                UserId.DEFAULT,
                SessionId.of(sessionId),
                message
        );
        ConversationTurn turn = ConversationTurn.received(userMessage);
        return new AgentRequest(turn, List.of(userMessage), "test system prompt");
    }
}
