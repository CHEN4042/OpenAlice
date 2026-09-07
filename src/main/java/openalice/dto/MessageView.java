package openalice.dto;

import java.time.Instant;
import openalice.model.ChatMessage;

public record MessageView(
        String id,
        String role,
        String content,
        Instant timestamp
) {
    public static MessageView from(ChatMessage message) {
        return new MessageView(
                message.id(),
                message.role().name().toLowerCase(),
                message.content(),
                message.timestamp()
        );
    }
}

