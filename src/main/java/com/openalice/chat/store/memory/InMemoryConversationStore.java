package com.openalice.chat.store.memory;

import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
