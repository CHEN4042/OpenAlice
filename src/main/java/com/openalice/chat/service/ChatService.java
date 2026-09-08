package com.openalice.chat.service;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentExecutor;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.MessageView;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 一轮 /chat 的业务编排。
 *
 * <p>完整的一轮在 {@link SessionCoordinator} 内串行执行：追加 USER 消息 →
 * 组装上下文 → 调用 agent（{@link AgentExecutor}）→ 追加 ASSISTANT 消息，
 * 同一会话不会被并发插入打乱；agent 抛错时兜成 {@link ErrorEvent} 继续走 SSE。
 * Controller 只负责把 {@link AgentEvent} 翻译成 SSE。</p>
 */
@Service
public class ChatService {

    /** 单用户 API 的默认内部用户；存储层仍按行保留 userId，为将来认证预留。 */
    private static final String DEFAULT_USER_ID = "openalice-user";

    private static final int HISTORY_LIMIT = 100;

    private final AgentExecutor agentExecutor;
    private final ConversationStore conversationStore;
    private final ContextAssembler contextAssembler;
    private final SessionCoordinator sessionCoordinator;

    public ChatService(
            AgentExecutor agentExecutor,
            ConversationStore conversationStore,
            ContextAssembler contextAssembler,
            SessionCoordinator sessionCoordinator
    ) {
        this.agentExecutor = agentExecutor;
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

            return agentExecutor.stream(agentRequest)
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
                    .onErrorResume(error -> Flux.just(new ErrorEvent(safeErrorMessage(error))));
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
        return requireText(value, "sessionId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String safeErrorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "chat failed" : message;
    }
}
