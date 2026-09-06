package openalice.server.api.dto;

public record ChatReply(
        String userId,
        String sessionId,
        String reply
) {
}

