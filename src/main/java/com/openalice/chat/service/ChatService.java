package com.openalice.chat.service;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.agent.runtime.AgentRuntime;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.ChatResponse;
import com.openalice.dto.MessageView;
import com.openalice.model.ChatMessage;
import com.openalice.model.ConversationTurn;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import com.openalice.chat.store.ConversationStore;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Chat orchestration for one turn.
 *
 * <p>The complete turn runs under SessionCoordinator: USER append, history
 * assembly, agent call, ASSISTANT append, and lifecycle transition are serialized
 * for the same session. The controller only translates AgentEvents to SSE.</p>
 */
@Service
public class ChatService {

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
        SessionId sessionId = SessionId.of(request.sessionId());
        ChatMessage userMessage = ChatMessage.user(UserId.DEFAULT, sessionId, request.message());
        ConversationTurn receivedTurn = ConversationTurn.received(userMessage);

        return sessionCoordinator.serialize(sessionId, Flux.defer(() -> {
            conversationStore.append(userMessage);
            ConversationTurn runningTurn = receivedTurn.running();
            AgentRequest agentRequest = contextAssembler.assemble(runningTurn, userMessage);
            AtomicReference<ConversationTurn> currentTurn = new AtomicReference<>(runningTurn);

            return agentRuntime.stream(agentRequest)
                    .concatMap(event -> {
                        if (event instanceof TextDeltaEvent) {
                            return Flux.just(event);
                        }
                        if (event instanceof DoneEvent doneEvent) {
                            ChatMessage assistantMessage = ChatMessage.assistant(
                                    UserId.DEFAULT,
                                    sessionId,
                                    doneEvent.reply()
                            );
                            conversationStore.append(assistantMessage);
                            currentTurn.set(currentTurn.get().completed(assistantMessage.id()));
                            return Flux.just(event);
                        }
                        if (event instanceof ErrorEvent errorEvent) {
                            currentTurn.set(currentTurn.get().failed(errorEvent.error()));
                            return Flux.just(event);
                        }
                        return Flux.error(new IllegalStateException("unsupported agent event"));
                    })
                    .onErrorResume(error -> {
                        String message = safeErrorMessage(error);
                        currentTurn.set(currentTurn.get().failed(message));
                        return Flux.just(new ErrorEvent(message));
                    });
        }));
    }

    /** Blocking convenience method; the HTTP API currently uses SSE only. */
    public ChatResponse chat(ChatRequest request) {
        List<AgentEvent> events = stream(request).collectList().block();
        String reply = events == null ? null : events.stream()
                .filter(DoneEvent.class::isInstance)
                .map(DoneEvent.class::cast)
                .map(DoneEvent::reply)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("chat did not complete"));
        return new ChatResponse(request.sessionId(), reply);
    }

    public List<MessageView> history(String sessionId) {
        SessionId parsedSessionId = SessionId.of(sessionId);
        List<ChatMessage> messages = sessionCoordinator.serialize(
                        parsedSessionId,
                        Mono.fromCallable(() -> conversationStore.history(
                                UserId.DEFAULT,
                                parsedSessionId,
                                HISTORY_LIMIT
                        ))
                )
                .single()
                .block();
        return Objects.requireNonNullElse(messages, List.<ChatMessage>of()).stream()
                .map(MessageView::from)
                .toList();
    }

    private static void requireRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        requireText(request.sessionId(), "sessionId");
        requireText(request.message(), "message");
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
