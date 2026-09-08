package com.openalice.dto;

/**
 * 单轮对话请求体。
 *
 * <p>单用户 API 不收 {@code userId}：服务端固定内部默认用户；客户端只需给
 * {@code sessionId}（会话标识）与 {@code message}（本条用户消息）。</p>
 */
public record ChatRequest(
        String sessionId,
        String message
) {
}
