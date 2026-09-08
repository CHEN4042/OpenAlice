package com.openalice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 对外 SSE 事件的最小集合：{@code text_delta} / {@code done} / {@code error}。
 *
 * <p>外部 API 是单用户的，且每个流都属于某个会话，所以事件体里不重复
 * userId / sessionId。</p>
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
