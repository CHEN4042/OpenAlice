package com.openalice.chat.store;

import java.util.List;

/**
 * Conversation persistence boundary.
 *
 * <p>The store is the source of truth for business conversation history. The
 * AgentScope state store is only runtime scratch state and is cleared before each
 * call. The API is single-user, so history is keyed by {@code sessionId}; the
 * stored record still carries {@code userId} for future authentication.</p>
 */
public interface ConversationStore {

    void append(StoredMessage message);

    List<StoredMessage> history(String sessionId, int limit);
}
