package openalice.dto;

public record ChatReply(
        String userId,
        String sessionId,
        String reply
) {
}

