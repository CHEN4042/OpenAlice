package com.openalice.dto;

public record ChatRequest(
        String sessionId,
        String message
) {
}
