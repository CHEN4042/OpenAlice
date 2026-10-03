package io.openalice.execution.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AgentScopeRuntimeTest {

    @Test
    void deterministicModelFlowsThroughBareAgentAndEachExecutionHasIsolatedState() {
        RecordingModel model = new RecordingModel();
        AgentScopeRuntime runtime = new AgentScopeRuntime(() -> new AgentModelConnection(model));

        List<RuntimeEvent> first = runtime.execute(request("first")).collectList().block();
        List<RuntimeEvent> second = runtime.execute(request("second")).collectList().block();

        assertThat(first).anySatisfy(event -> assertThat(event)
                .isInstanceOfSatisfying(RuntimeEvent.Completed.class, completed ->
                        assertThat(completed.result().text()).isEqualTo("reply-1")));
        assertThat(second).anySatisfy(event -> assertThat(event)
                .isInstanceOfSatisfying(RuntimeEvent.Completed.class, completed ->
                        assertThat(completed.result().text()).isEqualTo("reply-2")));
        assertThat(model.inputs()).hasSize(2);
        assertThat(model.inputs().get(0))
                .extracting(Msg::getTextContent)
                .containsExactly("Test system prompt", "first");
        assertThat(model.inputs().get(1))
                .extracting(Msg::getTextContent)
                .containsExactly("Test system prompt", "second");
    }

    private static RuntimeRequest request(String text) {
        return new RuntimeRequest(
                UUID.randomUUID(),
                "test-user",
                "Test system prompt",
                List.of(new RuntimeMessage(RuntimeMessage.Role.USER, text)));
    }

    private static final class RecordingModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            int call = calls.incrementAndGet();
            inputs.add(List.copyOf(new ArrayList<>(messages)));
            return Flux.just(ChatResponse.builder()
                    .id("response-" + call)
                    .content(List.<ContentBlock>of(
                            TextBlock.builder().text("reply-" + call).build()))
                    .usage(new ChatUsage(1, 1, 2))
                    .build());
        }

        @Override
        public String getModelName() {
            return "deterministic-test-model";
        }

        List<List<Msg>> inputs() {
            return List.copyOf(inputs);
        }
    }
}
