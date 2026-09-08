package com.openalice.chat.service;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.runtime.AgentRuntime;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.MessageView;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Chat orchestration for one turn.
 *
 * <p>The complete turn runs under SessionCoordinator: USER append, history
 * assembly, agent call and ASSISTANT append are serialized for the same session.
 * The controller only translates AgentEvents to SSE.</p>
 */
@Service
public class ChatService {

    /** Single-user API default; the storage layer keeps it per row for future auth. */
    private static final String DEFAULT_USER_ID = "openalice-user";

    private static final int HISTORY_LIMIT = 100;

    private final AgentRuntime agentRuntime;
    private final ConversationStore conversationStore;
    private final ContextAssembler contextAssembler;
    private final SessionCoordinator sessionCoordinator;

    public ChatService(
            AgentRuntime agentRuntime,
            ConversationStore conversationStore,
            ContextAssembler contextAssembler,
            SessionCoordinator sessionCoordinator
    ) {
        this.agentRuntime = agentRuntime;
        this.conversationStore = conversationStore;
        this.contextAssembler = contextAssembler;
        this.sessionCoordinator = sessionCoordinator;
    }

    public Flux<AgentEvent> stream(ChatRequest request) {
        requireRequest(request);
        String sessionId = requireSessionId(request.sessionId());
        StoredMessage userMessage = StoredMessage.user(DEFAULT_USER_ID, sessionId, request.message());

        return sessionCoordinator.serialize(sessionId, Flux.defer(() -> {
            conversationStore.append(userMessage);
            AgentRequest agentRequest = contextAssembler.assemble(DEFAULT_USER_ID, sessionId);

            return agentRuntime.stream(agentRequest)
                    .concatMap(event -> {
                        if (event instanceof DoneEvent doneEvent) {
                            conversationStore.append(StoredMessage.assistant(
                                    DEFAULT_USER_ID,
                                    sessionId,
                                    doneEvent.reply()
                            ));
                        }
                        return Flux.just(event);
                    })
                    .onErrorResume(error ->
                            Flux.just(new ErrorEvent(safeErrorMessage(error))));
        }));
    }

    public List<MessageView> history(String sessionId) {
        String parsedSessionId = requireSessionId(sessionId);
        List<StoredMessage> stored = sessionCoordinator.serialize(
                        parsedSessionId,
                        Mono.fromCallable(() -> conversationStore.history(parsedSessionId, HISTORY_LIMIT))
                )
                .single()
                .block();
        return (stored == null ? List.<StoredMessage>of() : stored).stream()
                .map(message -> new MessageView(
                        message.id(),
                        message.role().wireValue(),
                        message.content(),
                        message.createdAt()
                ))
                .toList();
    }

    private static void requireRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        requireText(request.sessionId(), "sessionId");
        requireText(request.message(), "message");
    }

    private static String requireSessionId(String value) {
        requireText(value, "sessionId");
        return value.trim();
    }

    private static String safeErrorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "chat failed" : message;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
