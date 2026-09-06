package openalice.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChatMessage(
        String id,
        MessageRole role,
        String content,
        UserId userId,
        SessionId sessionId,
        Instant timestamp
) {
    public ChatMessage {
        id = normalize(id);
        role = Objects.requireNonNull(role, "role must not be null");
        content = Objects.requireNonNull(content, "content must not be null");
        userId = UserId.require(userId);
        sessionId = SessionId.require(sessionId);
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public static ChatMessage user(UserId userId, SessionId sessionId, String content) {
        return new ChatMessage(null, MessageRole.USER, content, userId, sessionId, null);
    }

    public static ChatMessage assistant(UserId userId, SessionId sessionId, String content) {
        return new ChatMessage(null, MessageRole.ASSISTANT, content, userId, sessionId, null);
    }

    public ChatMessage withRole(MessageRole newRole) {
        return new ChatMessage(id, newRole, content, userId, sessionId, timestamp);
    }

    private static String normalize(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }
}

