package com.openalice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Minimal SSE event set: text_delta, done, error. The external API is
 * single-user, so it intentionally does not expose a userId.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        String type,
        String sessionId,
        String delta,
        String reply,
        String error
) {

    public static ChatStreamEvent textDelta(String sessionId, String delta) {
        return new ChatStreamEvent("text_delta", sessionId, delta, null, null);
    }

    public static ChatStreamEvent done(String sessionId, String reply) {
        return new ChatStreamEvent("done", sessionId, null, reply, null);
    }

    public static ChatStreamEvent error(String sessionId, String error) {
        return new ChatStreamEvent("error", sessionId, null, null, error);
    }
}
