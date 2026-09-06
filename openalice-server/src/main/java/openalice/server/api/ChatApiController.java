package openalice.server.api;

import java.util.List;
import openalice.core.domain.ChatMessage;
import openalice.core.domain.SessionId;
import openalice.core.domain.UserId;
import openalice.core.port.MemoryPort;
import openalice.server.api.dto.ChatReply;
import openalice.server.api.dto.ChatRequest;
import openalice.server.api.dto.MessageView;
import openalice.agent.runtime.AgentRuntime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ChatApiController {

    private final AgentRuntime agentRuntime;
    private final MemoryPort memoryPort;

    ChatApiController(AgentRuntime agentRuntime, MemoryPort memoryPort) {
        this.agentRuntime = agentRuntime;
        this.memoryPort = memoryPort;
    }

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    ChatReply chat(@RequestBody ChatRequest request) {
        requireText(request.userId(), "userId");
        requireText(request.sessionId(), "sessionId");
        requireText(request.message(), "message");

        ChatMessage userMessage = ChatMessage.user(
                UserId.of(request.userId()),
                SessionId.of(request.sessionId()),
                request.message()
        );
        var result = agentRuntime.chat(userMessage);
        return new ChatReply(
                result.userId().value(),
                result.sessionId().value(),
                result.reply()
        );
    }

    @GetMapping("/users/{userId}/sessions/{sessionId}/messages")
    List<MessageView> history(
            @PathVariable String userId,
            @PathVariable String sessionId
    ) {
        return memoryPort.history(UserId.of(userId), SessionId.of(sessionId), 100).stream()
                .map(MessageView::from)
                .toList();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}

