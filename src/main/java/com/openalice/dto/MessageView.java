package com.openalice.dto;

import java.time.Instant;

/** 历史接口返回的单条消息视图。 */
public record MessageView(
        String id,
        String role,
        String content,
        Instant timestamp
) {
}
