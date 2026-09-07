package openalice.service;

import java.util.List;
import openalice.agent.runtime.AgentRuntime;
import openalice.agent.runtime.ChatResult;
import openalice.dto.ChatResponse;
import openalice.dto.ChatRequest;
import openalice.dto.MessageView;
import openalice.model.ChatMessage;
import openalice.model.SessionId;
import openalice.model.UserId;
import openalice.port.MemoryPort;
import org.springframework.stereotype.Service;

/**
 * 聊天业务编排（service 层）。controller 只负责接 HTTP，真正的业务步骤都在这里。
 *
 * <p>一次 {@code POST /chat} 的完整链路：</p>
 * <ol>
 *   <li>校验并构造用户消息 {@link ChatMessage}（model 值对象自带非空校验）；</li>
 *   <li>先把用户消息写入 memory（通过 {@link MemoryPort}，具体存哪由实现决定）；</li>
 *   <li>把用户消息交给 {@link AgentRuntime}，向模型要一句回复；</li>
 *   <li>再把助手回复也写入 memory，保证 history 两端都有；</li>
 *   <li>返回 {@link ChatResponse} 给 controller。</li>
 * </ol>
 */
@Service
public class ChatService {

    private static final int HISTORY_LIMIT = 100;

    private final AgentRuntime agentRuntime;
    private final MemoryPort memoryPort;

    public ChatService(AgentRuntime agentRuntime, MemoryPort memoryPort) {
        this.agentRuntime = agentRuntime;
        this.memoryPort = memoryPort;
    }

    public ChatResponse chat(ChatRequest request) {
        requireText(request.message(), "message");

        UserId userId = UserId.of(request.userId());
        SessionId sessionId = SessionId.of(request.sessionId());

        ChatMessage userMessage = ChatMessage.user(userId, sessionId, request.message());
        memoryPort.append(userMessage);

        ChatResult result = agentRuntime.chat(userMessage);
        memoryPort.append(ChatMessage.assistant(userId, sessionId, result.reply()));

        return new ChatResponse(userId.value(), sessionId.value(), result.reply());
    }

    public List<MessageView> history(String userId, String sessionId) {
        return memoryPort.history(UserId.of(userId), SessionId.of(sessionId), HISTORY_LIMIT).stream()
                .map(MessageView::from)
                .toList();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
