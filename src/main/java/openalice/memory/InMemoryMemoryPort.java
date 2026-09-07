package openalice.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import openalice.model.ChatMessage;
import openalice.model.SessionId;
import openalice.model.UserId;
import openalice.port.MemoryPort;

public final class InMemoryMemoryPort implements MemoryPort {

    private record SessionKey(UserId userId, SessionId sessionId) {
    }

    private final Map<SessionKey, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    @Override
    public void append(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        List<ChatMessage> messages = sessions.computeIfAbsent(
                new SessionKey(message.userId(), message.sessionId()),
                key -> new ArrayList<>()
        );
        synchronized (messages) {
            messages.add(message);
        }
    }

    @Override
    public List<ChatMessage> history(UserId userId, SessionId sessionId, int limit) {
        UserId.require(userId);
        SessionId.require(sessionId);
        if (limit <= 0) {
            return List.of();
        }

        List<ChatMessage> messages = sessions.getOrDefault(new SessionKey(userId, sessionId), List.of());
        synchronized (messages) {
            int fromIndex = Math.max(0, messages.size() - limit);
            return List.copyOf(messages.subList(fromIndex, messages.size()));
        }
    }
}

