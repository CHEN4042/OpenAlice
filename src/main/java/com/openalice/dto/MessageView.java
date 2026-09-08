package com.openalice.dto;

import java.time.Instant;

/** One stored message as returned by the history API. */
public record MessageView(
        String id,
        String role,
        String content,
        Instant timestamp
) {
}
