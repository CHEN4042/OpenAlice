package openalice.model;

import java.util.Objects;

public record SessionId(String value) {
    public SessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        value = value.trim();
    }

    public static SessionId of(String value) {
        return new SessionId(value);
    }

    public static SessionId require(SessionId sessionId) {
        return Objects.requireNonNull(sessionId, "sessionId must not be null");
    }
}

