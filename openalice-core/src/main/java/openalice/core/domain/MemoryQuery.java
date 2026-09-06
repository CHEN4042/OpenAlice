package openalice.core.domain;

public record MemoryQuery(UserId userId, SessionId sessionId, int limit) {
    public MemoryQuery {
        UserId.require(userId);
        SessionId.require(sessionId);
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
    }
}

