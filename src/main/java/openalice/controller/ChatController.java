package openalice.controller;

import java.util.List;
import openalice.dto.ChatResponse;
import openalice.dto.ChatRequest;
import openalice.dto.MessageView;
import openalice.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 入口。只做三件事：接收请求、转给 {@link ChatService}、把结果返回。
 * 不写业务逻辑——业务在 service 层。
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/users/{userId}/sessions/{sessionId}/messages")
    public List<MessageView> history(
            @PathVariable String userId,
            @PathVariable String sessionId
    ) {
        return chatService.history(userId, sessionId);
    }
}
