package openalice.server.api.dto;

public record ChatRequest(
        String userId,
        String sessionId,
        String message
) {
}

