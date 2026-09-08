package com.openalice.controller;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentExecutor;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.chat.service.ChatService;
import com.openalice.chat.service.ContextAssembler;
import com.openalice.chat.service.SessionCoordinator;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.memory.InMemoryConversationStore;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 测试：只拉起 Web 层（@WebMvcTest），业务链路用真实 ChatService +
 * 手写假 AgentExecutor 组装，验证 SSE 翻译、会话持久化与历史查询整条链路，
 * 全程不依赖网络与 api-key。
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldStreamChatThroughServiceAndMapToSse() throws Exception {
        var asyncResult = mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"sessionId":"default","message":"你好"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event:text_delta")
                .contains("\"type\":\"text_delta\"")
                .contains("event:done")
                .contains("\"reply\":\"收到：你好\"");

        mockMvc.perform(get("/api/v1/sessions/default/messages"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"role\":\"user\"")))
                .andExpect(content().string(containsString("\"content\":\"收到：你好\"")));
    }

    @Test
    void shouldRejectBlankMessageAsJson() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"default","message":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankMessageAsSseError() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"sessionId":"default","message":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("message must not be blank")));
    }

    /** 测试装配：真实 ChatService 链路 + 手写假 AgentExecutor（不回显网络）。 */
    @TestConfiguration
    static class ChatServiceTestConfig {

        @Bean
        public ConversationStore conversationStore() {
            return new InMemoryConversationStore();
        }

        @Bean
        public ContextAssembler contextAssembler(ConversationStore store) {
            return new ContextAssembler(store, "You are Alice, a warm and attentive AI companion.", 20);
        }

        @Bean
        public SessionCoordinator sessionCoordinator() {
            return new SessionCoordinator();
        }

        @Bean
        public AgentExecutor agentExecutor() {
            return new EchoAgentExecutor();
        }

        @Bean
        public ChatService chatService(
                AgentExecutor executor,
                ConversationStore store,
                ContextAssembler assembler,
                SessionCoordinator coordinator
        ) {
            return new ChatService(executor, store, assembler, coordinator);
        }
    }

    /** 把最后一条 USER 消息回显成「收到：…」的假执行器，用于打通整条链路。 */
    private static final class EchoAgentExecutor implements AgentExecutor {

        @Override
        public Flux<AgentEvent> stream(AgentRequest request) {
            String lastUserMessage = request.conversationContext()
                    .get(request.conversationContext().size() - 1)
                    .content();
            return Flux.just(
                    new TextDeltaEvent("收到："),
                    new TextDeltaEvent(lastUserMessage),
                    new DoneEvent("收到：" + lastUserMessage)
            );
        }
    }
}
