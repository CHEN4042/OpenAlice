package openalice.core.port;

import java.util.List;
import openalice.core.domain.ChatMessage;
import openalice.core.domain.SessionId;
import openalice.core.domain.UserId;

public interface MemoryPort {
    void append(ChatMessage message);

    List<ChatMessage> history(UserId userId, SessionId sessionId, int limit);
}

