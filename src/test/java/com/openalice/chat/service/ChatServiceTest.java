package com.openalice.chat.service;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.agent.runtime.AgentRuntime;
import com.openalice.agent.runtime.AgentRuntimePropertiesFixture;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.chat.store.memory.InMemoryConversationStore;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.MessageView;
import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatServiceTest {

    private static final class RecordingAgentRuntime implements AgentRuntime {
        private final List<AgentRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public Flux<AgentEvent> stream(AgentRequest request) {
            requests.add(request);
            return Flux.just(
                    new TextDeltaEvent("收到："),
                    new TextDeltaEvent(lastUserMessage(request)),
                    new DoneEvent("收到：" + lastUserMessage(request))
            );
        }

        private static String lastUserMessage(AgentRequest request) {
            return request.conversationContext()
                    .get(request.conversationContext().size() - 1)
                    .content();
        }
    }

    private static ChatService service(
            AgentRuntime runtime,
            ConversationStore store,
            int contextWindowSize
    ) {
        return new ChatService(
                runtime,
                store,
                new ContextAssembler(store, AgentRuntimePropertiesFixture.mock(), contextWindowSize),
                new SessionCoordinator()
        );
    }

    private static List<AgentEvent> complete(ChatService service, ChatRequest request) {
        return service.stream(request).collectList().block();
    }

    @Test
    void shouldPersistUserAndAssistantAroundAgentCall() {
        ConversationStore store = new InMemoryConversationStore();
        ChatService service = service(new RecordingAgentRuntime(), store, 20);

        complete(service, new ChatRequest("session", "你好"));

        assertThat(store.history("session", 10))
                .extracting(StoredMessage::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(store.history("session", 10))
                .extracting(StoredMessage::content)
                .containsExactly("你好", "收到：你好");
    }

    @Test
    void shouldAssembleExplicitContextBeforeAgentCall() {
        ConversationStore store = new InMemoryConversationStore();
        String sessionId = "session";
        store.append(StoredMessage.user("user-1", sessionId, "历史问题"));
        store.append(StoredMessage.assistant("user-1", sessionId, "历史回答"));
        RecordingAgentRuntime runtime = new RecordingAgentRuntime();
        ChatService service = service(runtime, store, 20);

        complete(service, new ChatRequest("session", "新问题"));

        AgentRequest request = runtime.requests.get(0);
        assertThat(request.userId()).isNotBlank();
        assertThat(request.sessionId()).isEqualTo("session");
        assertThat(request.systemPrompt()).isEqualTo(AgentRuntimePropertiesFixture.SYSTEM_PROMPT);
        assertThat(request.conversationContext())
                .extracting(ChatMessage::content)
                .containsExactly("历史问题", "历史回答", "新问题");
        assertThat(store.history(sessionId, 10))
                .extracting(StoredMessage::content)
                .containsExactly("历史问题", "历史回答", "新问题", "收到：新问题");
    }

    @Test
    void shouldEmitErrorEventWhenAgentStreamFails() {
        ConversationStore store = new InMemoryConversationStore();
        AgentRuntime failingRuntime = request -> Flux.error(new IllegalStateException("provider unavailable"));
        ChatService service = service(failingRuntime, store, 20);

        List<AgentEvent> events = complete(service, new ChatRequest("session", "hi"));

        assertThat(events).singleElement().isInstanceOf(ErrorEvent.class);
        assertThat(((ErrorEvent) events.get(0)).error()).isEqualTo("provider unavailable");
    }

    @Test
    void shouldExposeHistoryAsMessageViews() {
        ConversationStore store = new InMemoryConversationStore();
        ChatService service = service(new RecordingAgentRuntime(), store, 20);
        complete(service, new ChatRequest("session", "hi"));

        List<MessageView> views = service.history("session");
        assertThat(views).hasSize(2);
        assertThat(views.get(0).role()).isEqualTo("user");
        assertThat(views.get(1).role()).isEqualTo("assistant");
        assertThat(views.get(1).content()).isEqualTo("收到：hi");
    }

    @Test
    void shouldRejectBlankMessage() {
        ChatService service = service(
                new RecordingAgentRuntime(),
                new InMemoryConversationStore(),
                20
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.stream(new ChatRequest("session", " ")));
    }
}
