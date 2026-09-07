package openalice.agent.llm;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.util.List;
import java.util.Objects;
import reactor.core.publisher.Flux;

public final class DeterministicChatModel implements Model {

    private final String replyPrefix;

    public DeterministicChatModel(String replyPrefix) {
        this.replyPrefix = replyPrefix == null ? "收到：" : replyPrefix;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages,
            List<ToolSchema> tools,
            GenerateOptions options
    ) {
        Objects.requireNonNull(messages, "messages must not be null");
        String userText = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg message = messages.get(i);
            if (message.getRole() == MsgRole.USER && !message.getTextContent().isBlank()) {
                userText = message.getTextContent();
                break;
            }
        }

        TextBlock textBlock = TextBlock.builder()
                .text(replyPrefix + userText)
                .build();
        ChatResponse response = ChatResponse.builder()
                .content(List.of(textBlock))
                .usage(new ChatUsage(userText.length(), replyPrefix.length() + userText.length(), 0, 0.001))
                .finishReason("stop")
                .build();
        return Flux.just(response);
    }

    @Override
    public String getModelName() {
        return "openalice-deterministic-v1";
    }
}

