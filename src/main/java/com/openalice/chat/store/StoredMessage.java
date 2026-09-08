package com.openalice.chat.store;

import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One stored conversation record — a database row: the business {@link ChatMessage}
 * plus ownership ({@code userId} / {@code sessionId}) and storage metadata
 * ({@code id} / {@code createdAt}).
 *
 * <p>The single-user API always writes the service's default user; the {@code userId}
 * column is kept so the storage model does not change when authentication arrives.</p>
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
