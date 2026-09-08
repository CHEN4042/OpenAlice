package com.openalice.chat.store.memory;

import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ConversationStore} 的内存实现（按会话分桶）。
 *
 * <p>仅用于本地开发与测试；将来接 PostgreSQL 时新增一个同名端口实现并在
 * 组合根替换即可。</p>
 */
public final class InMemoryConversationStore implements ConversationStore {

    private final Map<String, List<StoredMessage>> sessions = new ConcurrentHashMap<>();

    @Override
    public void append(StoredMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        List<StoredMessage> messages = sessions.computeIfAbsent(
                message.sessionId(),
                key -> new ArrayList<>()
        );
        synchronized (messages) {
            messages.add(message);
        }
    }

    @Override
    public List<StoredMessage> history(String sessionId, int limit) {
        requireNotBlank(sessionId, "sessionId");
        if (limit <= 0) {
            return List.of();
        }
        List<StoredMessage> messages = sessions.getOrDefault(sessionId, List.of());
        synchronized (messages) {
            int fromIndex = Math.max(0, messages.size() - limit);
            return List.copyOf(messages.subList(fromIndex, messages.size()));
        }
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
