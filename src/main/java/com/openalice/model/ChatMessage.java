package com.openalice.model;

import java.util.Objects;

/**
 * 对话与 agent 共用的单条消息：谁说的（{@link MessageRole}）和说了什么
 * （{@code content}）。仅此而已——id、归属与时间戳属于存储记录
 * （{@code com.openalice.chat.store.StoredMessage}）。
 */
public record ChatMessage(MessageRole role, String content) {

    public ChatMessage {
        role = requireRole(role);
        content = requireNotBlank(content, "content");
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(MessageRole.ASSISTANT, content);
    }

    private static MessageRole requireRole(MessageRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
