package com.openalice.agent;

import com.openalice.model.ChatMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentScopeReActAgent 的单元测试：注入假 Model（不回网络、不需要 api-key），
 * 验证 AgentScope ReAct 引擎事件流到自有 {@link AgentEvent} 的映射。
 */
class AgentScopeReActAgentTest {

    private static final String USER_ID = "user-1";

    @Test
    void shouldStreamAgentEventsViaReactAgent() {
        try (AgentExecutor executor = executorWith(new EchoChatModel())) {
            List<AgentEvent> events = executor.stream(request("stream", "你好"))
                    .collectList()
                    .block();

            assertThat(events).extracting(event -> event.getClass().getSimpleName())
                    .contains("TextDeltaEvent", "DoneEvent");
            assertThat(events.stream().filter(TextDeltaEvent.class::isInstance)
                    .map(TextDeltaEvent.class::cast)
                    .map(TextDeltaEvent::delta)
                    .reduce("", String::concat))
                    .isEqualTo("echo:你好");
            assertThat(events.stream().filter(DoneEvent.class::isInstance)
                    .map(DoneEvent.class::cast)
                    .map(DoneEvent::reply)
                    .findFirst())
                    .contains("echo:你好");
        }
    }

    @Test
    void shouldKeepSessionsIsolatedUnderConcurrency() throws Exception {
        try (AgentExecutor executor = executorWith(new EchoChatModel())) {
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<List<AgentEvent>> alpha = pool.submit(
                        () -> executor.stream(request("alpha", "Alpha")).collectList().block()
                );
                Future<List<AgentEvent>> beta = pool.submit(
                        () -> executor.stream(request("beta", "Beta")).collectList().block()
                );
                assertThat(replyOf(alpha.get(10, TimeUnit.SECONDS))).isEqualTo("echo:Alpha");
                assertThat(replyOf(beta.get(10, TimeUnit.SECONDS))).isEqualTo("echo:Beta");
            } finally {
                pool.shutdownNow();
            }
        }
    }

    private static AgentExecutor executorWith(Model model) {
        return new AgentScopeReActAgent(
                "Alice",
                "OpenAlice test agent",
                "You are Alice, a warm and attentive AI companion.",
                Duration.ofSeconds(60),
                model
        );
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

    /** 把最后一条 USER 消息原样回显的假模型（不触发任何工具调用）。 */
    private static final class EchoChatModel implements Model {

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options
        ) {
            String lastUserText = messages.stream()
                    .filter(message -> message.getRole() == MsgRole.USER)
                    .map(Msg::getTextContent)
                    .filter(content -> !content.isBlank())
                    .reduce((first, second) -> second)
                    .orElse("");
            return Flux.just(ChatResponse.builder()
                    .id("echo-test-id")
                    .content(List.of(TextBlock.builder().text("echo:" + lastUserText).build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "echo-test";
        }
    }
}
