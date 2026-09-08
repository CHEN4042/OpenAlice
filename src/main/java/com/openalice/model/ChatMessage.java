package com.openalice.model;

import java.util.Objects;

/**
 * One dialogue message shared by chat and agent: who says it ({@link MessageRole})
 * and what is said ({@code content}). Nothing else — id, ownership and timestamps
 * belong to the stored record in {@code com.openalice.chat.store.StoredMessage}.
 */
public record ChatMessage(MessageRole role, String content) {

    public ChatMessage {
        role = Objects.requireNonNull(role, "role must not be null");
        content = requireNotBlank(content, "content");
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(MessageRole.ASSISTANT, content);
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
