package com.openalice.chat.store;

import com.openalice.model.ChatMessage;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import java.util.List;

/**
 * Conversation persistence boundary.
 *
 * <p>The store is the source of truth for business conversation history. The
 * AgentScope state store is only runtime scratch state and is cleared before
 * each call.</p>
 */
public interface ConversationStore {
    void append(ChatMessage message);

    List<ChatMessage> history(UserId userId, SessionId sessionId, int limit);
}
