package com.openalice.controller;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.chat.service.ChatService;
import com.openalice.dto.ChatRequest;
import com.openalice.dto.ChatStreamEvent;
import com.openalice.dto.MessageView;
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
 * HTTP/SSE 边界层：只负责把 {@link AgentEvent} 翻译成 SSE 事件，
 * 不做任何业务编排（业务在 ChatService）。
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
        return chatService.stream(request)
                .map(ChatController::toStreamEvent)
                .map(event -> ServerSentEvent.<ChatStreamEvent>builder()
                        .event(event.type())
                        .data(event)
                        .build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<MessageView> history(@PathVariable String sessionId) {
        return chatService.history(sessionId);
    }

    /** 把 agent 域事件映射为传输层 SSE 事件。 */
    private static ChatStreamEvent toStreamEvent(AgentEvent event) {
        return switch (event) {
            case TextDeltaEvent deltaEvent -> ChatStreamEvent.textDelta(deltaEvent.delta());
            case DoneEvent doneEvent -> ChatStreamEvent.done(doneEvent.reply());
            case ErrorEvent errorEvent -> ChatStreamEvent.error(errorEvent.error());
        };
    }
}
