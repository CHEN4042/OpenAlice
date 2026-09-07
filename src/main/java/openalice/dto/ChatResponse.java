package openalice.dto;

public record ChatResponse(
        String userId,
        String sessionId,
        String reply
) {
}

