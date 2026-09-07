package com.openalice.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable lifecycle record for one conversation turn.
 *
 * <p>The turn is created when a user request is accepted, then transitions to
 * RUNNING before the agent is invoked. COMPLETED and FAILED are terminal states
 * used by the service orchestration.</p>
 */
public record ConversationTurn(
        String id,
        UserId userId,
        SessionId sessionId,
        String userMessageId,
        String assistantMessageId,
        TurnStatus status,
        Instant receivedAt,
        Instant startedAt,
        Instant completedAt,
        String error
) {

    public ConversationTurn {
        id = normalizeId(id);
        userId = UserId.require(userId);
        sessionId = SessionId.require(sessionId);
        userMessageId = normalize(userMessageId);
        assistantMessageId = normalize(assistantMessageId);
        status = Objects.requireNonNull(status, "status must not be null");
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }

    public static ConversationTurn received(ChatMessage userMessage) {
        if (userMessage == null || userMessage.role() != MessageRole.USER) {
            throw new IllegalArgumentException("a turn must start from a USER message");
        }
        return new ConversationTurn(
                null,
                userMessage.userId(),
                userMessage.sessionId(),
                userMessage.id(),
                null,
                TurnStatus.RECEIVED,
                userMessage.timestamp(),
                null,
                null,
                null
        );
    }

    public ConversationTurn running() {
        requireStatus(TurnStatus.RECEIVED);
        return new ConversationTurn(
                id, userId, sessionId, userMessageId, assistantMessageId,
                TurnStatus.RUNNING, receivedAt, Instant.now(), null, null
        );
    }

    public ConversationTurn completed(String assistantMessageId) {
        requireStatus(TurnStatus.RUNNING);
        return new ConversationTurn(
                id, userId, sessionId, userMessageId, normalize(assistantMessageId),
                TurnStatus.COMPLETED, receivedAt, startedAt, Instant.now(), null
        );
    }

    public ConversationTurn failed(String errorMessage) {
        requireStatus(TurnStatus.RECEIVED, TurnStatus.RUNNING);
        return new ConversationTurn(
                id, userId, sessionId, userMessageId, assistantMessageId,
                TurnStatus.FAILED, receivedAt, startedAt, Instant.now(),
                normalizeMessage(errorMessage)
        );
    }

    public ConversationTurn cancelled() {
        requireStatus(TurnStatus.RECEIVED, TurnStatus.RUNNING);
        return new ConversationTurn(
                id, userId, sessionId, userMessageId, assistantMessageId,
                TurnStatus.CANCELLED, receivedAt, startedAt, Instant.now(), null
        );
    }

    private void requireStatus(TurnStatus... allowed) {
        for (TurnStatus candidate : allowed) {
            if (status == candidate) {
                return;
            }
        }
        throw new IllegalStateException("cannot transition from " + status);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeMessage(String value) {
        return value == null || value.isBlank() ? "chat failed" : value;
    }

    private static String normalizeId(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
