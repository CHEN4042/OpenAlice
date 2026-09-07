package openalice.service;

import java.util.List;
import openalice.agent.runtime.AgentRuntime;
import openalice.agent.runtime.ChatResult;
import openalice.dto.ChatResponse;
import openalice.dto.ChatRequest;
import openalice.dto.MessageView;
import openalice.memory.InMemoryMemoryPort;
import openalice.model.ChatMessage;
import openalice.enums.MessageRole;
import openalice.model.SessionId;
import openalice.model.UserId;
import openalice.port.MemoryPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatServiceTest {

    /** 假 agent：不接模型，回显「收到：」+ 用户消息，便于只测编排顺序。 */
    private static final class EchoAgentRuntime implements AgentRuntime {
        @Override
        public ChatResult chat(ChatMessage userMessage) {
            return new ChatResult(
                    userMessage.userId(),
                    userMessage.sessionId(),
                    "收到：" + userMessage.content()
            );
        }
    }

    @Test
    void shouldPersistUserAndAssistantAroundAgentCall() {
        MemoryPort memory = new InMemoryMemoryPort();
        ChatService service = new ChatService(new EchoAgentRuntime(), memory);

        ChatResponse reply = service.chat(new ChatRequest("user", "session", "你好"));

        assertThat(reply.reply()).isEqualTo("收到：你好");
        List<ChatMessage> history = memory.history(UserId.of("user"), SessionId.of("session"), 10);
        assertThat(history).extracting(ChatMessage::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(history).extracting(ChatMessage::content)
                .containsExactly("你好", "收到：你好");
    }

    @Test
    void shouldExposeHistoryAsMessageViews() {
        ChatService service = new ChatService(new EchoAgentRuntime(), new InMemoryMemoryPort());
        service.chat(new ChatRequest("user", "session", "hi"));

        List<MessageView> views = service.history("user", "session");
        assertThat(views).hasSize(2);
        assertThat(views.get(0).role()).isEqualTo("user");
        assertThat(views.get(1).content()).isEqualTo("收到：hi");
    }

    @Test
    void shouldRejectBlankMessage() {
        ChatService service = new ChatService(new EchoAgentRuntime(), new InMemoryMemoryPort());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.chat(new ChatRequest("user", "session", " ")));
    }
}
