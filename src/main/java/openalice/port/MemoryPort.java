package openalice.port;

import java.util.List;
import openalice.model.ChatMessage;
import openalice.model.SessionId;
import openalice.model.UserId;

public interface MemoryPort {
    void append(ChatMessage message);

    List<ChatMessage> history(UserId userId, SessionId sessionId, int limit);
}

