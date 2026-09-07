package com.openalice.controller;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.ChatStreamEvent;
import com.openalice.dto.MessageView;
import com.openalice.service.ChatService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * HTTP/SSE boundary. It maps AgentEvents to transport events and contains no
 * business orchestration.
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<ServerSentEvent<ChatStreamEvent>> chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();
        return chatService.stream(request)
                .map(event -> ServerSentEvent.<ChatStreamEvent>builder()
                        .event(eventName(event))
                        .data(toStreamEvent(sessionId, event))
                        .build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<MessageView> history(@PathVariable String sessionId) {
        return chatService.history(sessionId);
    }

    private static ChatStreamEvent toStreamEvent(String sessionId, AgentEvent event) {
        if (event instanceof TextDeltaEvent deltaEvent) {
            return ChatStreamEvent.textDelta(sessionId, deltaEvent.delta());
        }
        if (event instanceof DoneEvent doneEvent) {
            return ChatStreamEvent.done(sessionId, doneEvent.reply());
        }
        if (event instanceof ErrorEvent errorEvent) {
            return ChatStreamEvent.error(sessionId, errorEvent.error());
        }
        throw new IllegalArgumentException("unsupported agent event");
    }

    private static String eventName(AgentEvent event) {
        if (event instanceof TextDeltaEvent) {
            return "text_delta";
        }
        if (event instanceof DoneEvent) {
            return "done";
        }
        if (event instanceof ErrorEvent) {
            return "error";
        }
        throw new IllegalArgumentException("unsupported agent event");
    }
}
