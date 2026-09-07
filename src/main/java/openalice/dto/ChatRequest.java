package openalice.dto;

public record ChatRequest(
        String userId,
        String sessionId,
        String message
) {
}

