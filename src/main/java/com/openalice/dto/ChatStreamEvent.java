package com.openalice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Minimal SSE event set: text_delta, done, error. The external API is
 * single-user and each stream belongs to one session, so neither userId nor
 * sessionId is repeated inside the event payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        String type,
        String delta,
        String reply,
        String error
) {

    public static ChatStreamEvent textDelta(String delta) {
        return new ChatStreamEvent("text_delta", delta, null, null);
    }

    public static ChatStreamEvent done(String reply) {
        return new ChatStreamEvent("done", null, reply, null);
    }

    public static ChatStreamEvent error(String error) {
        return new ChatStreamEvent("error", null, null, error);
    }
}
