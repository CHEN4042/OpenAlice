package com.openalice.chat.service;

import com.openalice.agent.AgentRequest;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 从会话存储显式组装 agent 入参。
 *
 * <p>职责是让「历史拼装」既不在 Controller 也不在 Agent 实现里发生：
 * 存储仍是业务历史的唯一事实来源，这里把最近 N 条历史 + 系统提示词（来自
 * {@code openalice.agent.system-prompt} 配置）转成 {@link AgentRequest}。
 * 调用方运行在 {@link SessionCoordinator} 内，因此最新一条已存消息必然是
 * 本轮刚追加的 USER 消息。</p>
 */
@Service
public class ContextAssembler {

    private final ConversationStore conversationStore;
    private final String systemPrompt;
    private final int contextWindowSize;

    public ContextAssembler(
            ConversationStore conversationStore,
            @Value("${openalice.agent.system-prompt}") String systemPrompt,
            @Value("${openalice.agent.context-window-size:20}") int contextWindowSize
    ) {
        this.conversationStore = conversationStore;
        this.systemPrompt = systemPrompt;
        this.contextWindowSize = contextWindowSize;
        if (contextWindowSize <= 0) {
            throw new IllegalArgumentException("contextWindowSize must be positive");
        }
    }

    public AgentRequest assemble(String userId, String sessionId) {
        List<StoredMessage> stored = conversationStore.history(sessionId, contextWindowSize);
        if (stored.isEmpty() || stored.get(stored.size() - 1).role() != MessageRole.USER) {
            throw new IllegalStateException("current USER message is not present in conversation context");
        }
        List<ChatMessage> context = stored.stream().map(StoredMessage::message).toList();
        return new AgentRequest(userId, sessionId, context, systemPrompt);
    }
}
