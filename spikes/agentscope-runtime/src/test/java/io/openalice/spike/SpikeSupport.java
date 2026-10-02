package io.openalice.spike;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import reactor.core.publisher.Flux;

final class SpikeSupport {
    private SpikeSupport() {}

    static Msg user(String text) {
        return message("user", MsgRole.USER, text);
    }

    static Msg assistant(String text) {
        return message("assistant", MsgRole.ASSISTANT, text);
    }

    static Msg message(String name, MsgRole role, String text) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(TextBlock.builder().text(text).build())
                .build();
    }

    static ChatResponse textResponse(String text) {
        return ChatResponse.builder()
                .id("response-" + UUID.randomUUID())
                .content(List.<ContentBlock>of(TextBlock.builder().text(text).build()))
                .usage(new ChatUsage(1, 1, 2))
                .build();
    }

    static ChatResponse toolResponse(String name, String id, Map<String, Object> input) {
        return ChatResponse.builder()
                .id("response-" + UUID.randomUUID())
                .content(
                        List.<ContentBlock>of(
                                ToolUseBlock.builder()
                                        .name(name)
                                        .id(id)
                                        .input(input)
                                        .content(JsonUtils.getJsonCodec().toJson(input))
                                        .build()))
                .usage(new ChatUsage(1, 1, 2))
                .build();
    }

    static long occurrences(List<Msg> messages, String exactText) {
        return messages.stream()
                .filter(message -> exactText.equals(message.getTextContent()))
                .count();
    }

    static final class RecordingModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();
        private final BiFunction<Integer, List<Msg>, Flux<ChatResponse>> script;

        RecordingModel(BiFunction<Integer, List<Msg>, Flux<ChatResponse>> script) {
            this.script = script;
        }

        static RecordingModel textReplies() {
            return new RecordingModel(
                    (call, messages) -> Flux.just(textResponse("reply-" + call)));
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            int call = calls.incrementAndGet();
            inputs.add(List.copyOf(new ArrayList<>(messages)));
            return script.apply(call, messages);
        }

        @Override
        public String getModelName() {
            return "openalice-spike-recording-model";
        }

        int callCount() {
            return calls.get();
        }

        List<List<Msg>> inputs() {
            return List.copyOf(inputs);
        }
    }
}
