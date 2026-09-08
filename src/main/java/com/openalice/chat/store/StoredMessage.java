package com.openalice.chat.store;

import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 一条已存储的会话记录（对应数据库一行）：业务消息 {@link ChatMessage}
 * 加上归属（{@code userId} / {@code sessionId}）与存储元数据
 * （{@code id} / {@code createdAt}）。
 *
 * <p>单用户 API 始终写入 service 的默认用户；保留 {@code userId} 列是为了
 * 将来接入认证时存储模型不必再改。</p>
 */
public record StoredMessage(
        String id,
        String userId,
        String sessionId,
        ChatMessage message,
        Instant createdAt
) {

    public StoredMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
        userId = requireNotBlank(userId, "userId");
        sessionId = requireNotBlank(sessionId, "sessionId");
        message = Objects.requireNonNull(message, "message must not be null");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static StoredMessage user(String userId, String sessionId, String content) {
        return new StoredMessage(null, userId, sessionId, ChatMessage.user(content), null);
    }

    public static StoredMessage assistant(String userId, String sessionId, String content) {
        return new StoredMessage(null, userId, sessionId, ChatMessage.assistant(content), null);
    }

    public MessageRole role() {
        return message.role();
    }

    public String content() {
        return message.content();
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
