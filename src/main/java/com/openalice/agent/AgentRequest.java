package com.openalice.agent;

import com.openalice.model.ChatMessage;
import java.util.List;
import java.util.Objects;

/**
 * 传给智能体运行时的显式入参。
 *
 * <p>{@code userId} / {@code sessionId} 标识 RuntimeContext；单用户 API 始终使用
 * service 的默认用户。{@code conversationContext} 是最近一段业务历史（含当前这条
 * USER 消息）。{@code systemPrompt} 由 service 层单独拼好，agent 适配器自己不读
 * 存储。</p>
 */
public record AgentRequest(
        String userId,
        String sessionId,
        List<ChatMessage> conversationContext,
        String systemPrompt
) {

    public AgentRequest {
        userId = requireNotBlank(userId, "userId");
        sessionId = requireNotBlank(sessionId, "sessionId");
        conversationContext = List.copyOf(Objects.requireNonNull(
                conversationContext,
                "conversationContext must not be null"
        ));
        systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt must not be null").trim();
        if (systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
