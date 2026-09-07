package com.openalice.model;

import java.util.Objects;

public record UserId(String value) {

    /** Single-user deployment: the API does not expose or accept a user id. */
    public static final UserId DEFAULT = new UserId("openalice-user");

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    public static UserId require(UserId userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }
}
