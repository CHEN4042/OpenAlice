package com.openalice.agent;

import com.openalice.agent.tool.AgentToolkit;
import com.openalice.llm.LlmModelFactory;
import com.openalice.llm.LlmSettings;
import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Flux;

/**
 * 基于 AgentScope ReAct 引擎的应用 Agent 实现（默认且唯一的 {@link AgentExecutor}）。
 *
 * <p>实现消费已组装好的 {@link AgentRequest}：每次调用前先清空 AgentScope 在该会话的
 * 进程内暂存状态（避免业务历史与框架记忆叠加），再以显式上下文驱动 ReActAgent——模型在
 * “推理 → 调用工具 → 观察结果”的循环里迭代，直到给出最终回答或达到最大迭代次数。
 * ReAct 循环本身由 AgentScope 框架提供，这里只负责装配与事件翻译。</p>
 *
 * <p>由组合根（{@code com.openalice.config.OpenAliceConfiguration}）用外部配置构造；
 * 包内另提供注入 {@link Model} 的构造重载，供测试用假模型验证事件映射，避免依赖真实
 * 网络与 api-key。</p>
 */
public final class AgentScopeReActAgent implements AgentExecutor {

    /** ReAct 循环最大迭代次数：给“思考 + 工具调用”留足轮次后收敛。 */
    private static final int MAX_ITERATIONS = 8;

    private final ReActAgent agent;
    private final Duration timeout;

    /** 生产构造：由组合根把外部配置（agent 行为 + LLM 接线）解析成引擎。 */
    public AgentScopeReActAgent(
            String agentName,
            String description,
            String systemPrompt,
            Duration timeout,
            LlmSettings llmSettings
    ) {
        this(agentName, description, systemPrompt, timeout, LlmModelFactory.create(llmSettings));
    }

    /** 测试构造：直接注入假 {@link Model}，跳过网络与 api-key。 */
    AgentScopeReActAgent(
            String agentName,
            String description,
            String systemPrompt,
            Duration timeout,
            Model model
    ) {
        Objects.requireNonNull(agentName, "agentName must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        this.timeout = timeout;
        this.agent = ReActAgent.builder()
                .name(agentName)
                .description(description)
                .sysPrompt(systemPrompt)
                .model(model)
                .toolkit(AgentToolkit.create())
                .stateStore(new InMemoryAgentStateStore())
                .maxIters(MAX_ITERATIONS)
                .build();
    }

    @Override
    public Flux<AgentEvent> stream(AgentRequest request) {
        validate(request);
        RuntimeContext context = RuntimeContext.builder()
                .userId(request.userId())
                .sessionId(request.sessionId())
                .build();

        return Flux.defer(() -> {
            agent.clearContext(context);
            AtomicBoolean emittedDelta = new AtomicBoolean();
            AtomicReference<String> finalReply = new AtomicReference<>();
            StringBuilder streamedReply = new StringBuilder();

            Flux<AgentEvent> deltas = agent
                    .streamEvents(toAgentMessages(request), context)
                    .handle((event, sink) -> {
                        if (event instanceof TextBlockDeltaEvent textEvent
                                && textEvent.getDelta() != null
                                && !textEvent.getDelta().isEmpty()) {
                            emittedDelta.set(true);
                            streamedReply.append(textEvent.getDelta());
                            sink.next(new TextDeltaEvent(textEvent.getDelta()));
                        } else if (event instanceof AgentResultEvent resultEvent) {
                            finalReply.set(resultEvent.getResult().getTextContent());
                        }
                    });

            return deltas.concatWith(Flux.defer(() -> {
                String reply = firstNonBlank(finalReply.get(), streamedReply.toString());
                if (reply == null) {
                    throw new IllegalStateException("AgentScope returned no response");
                }
                if (emittedDelta.get()) {
                    return Flux.just(new DoneEvent(reply));
                }
                return Flux.just(new TextDeltaEvent(reply), new DoneEvent(reply));
            }));
        })
                .timeout(timeout)
                .onErrorResume(error -> Flux.just(new ErrorEvent(safeErrorMessage(error))));
    }

    private static List<Msg> toAgentMessages(AgentRequest request) {
        List<Msg> messages = new ArrayList<>();
        for (ChatMessage message : request.conversationContext()) {
            messages.add(Msg.builder()
                    .name(message.role().wireValue())
                    .role(toAgentRole(message.role()))
                    .textContent(message.content())
                    .build());
        }
        return List.copyOf(messages);
    }

    private static MsgRole toAgentRole(MessageRole role) {
        return switch (role) {
            case USER -> MsgRole.USER;
            case ASSISTANT -> MsgRole.ASSISTANT;
            case SYSTEM -> MsgRole.SYSTEM;
        };
    }

    private static void validate(AgentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.conversationContext().isEmpty()
                || request.conversationContext().get(request.conversationContext().size() - 1).role() != MessageRole.USER) {
            throw new IllegalArgumentException("agent request must end with the current USER message");
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static String safeErrorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "agent execution failed" : message;
    }
}
