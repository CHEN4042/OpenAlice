package com.openalice.agent.runtime;

import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.agent.ErrorEvent;
import com.openalice.agent.TextDeltaEvent;
import com.openalice.agent.llm.LlmModelFactory;
import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AgentScope adapter.
 *
 * <p>The adapter receives an already-assembled {@link AgentRequest}. It clears
 * AgentScope's per-session scratch state and passes the explicit business
 * context, preventing double history accumulation.</p>
 */
final class AgentScopeAgentRuntime implements AgentRuntime {

    private final HarnessAgent agent;
    private final Duration timeout;

    AgentScopeAgentRuntime(AgentRuntimeProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.timeout = properties.timeout();
        this.agent = HarnessAgent.builder()
                .name(properties.agentName())
                .description(properties.description())
                .sysPrompt(properties.systemPrompt())
                .model(LlmModelFactory.create(properties))
                .stateStore(new InMemoryAgentStateStore())
                .workspace(properties.workspace())
                .maxIters(1)
                .enableAgentTracingLog(false)
                .disableFilesystemTools()
                .disableShellTool()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableWorkspaceContext()
                .disableAtPathExpansion()
                .disableSubagents()
                .disableDynamicSubagents()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .disableToolsConfig()
                .disableCompaction()
                .disableToolResultEviction()
                .build();
    }

    @Override
    public Flux<com.openalice.agent.AgentEvent> stream(AgentRequest request) {
        validate(request);
        RuntimeContext context = RuntimeContext.builder()
                .userId(request.turn().userId().value())
                .sessionId(request.turn().sessionId().value())
                .build();

        return Flux.defer(() -> {
            agent.clearContext(context);
            AtomicBoolean emittedDelta = new AtomicBoolean();
            AtomicReference<String> finalReply = new AtomicReference<>();
            StringBuilder streamedReply = new StringBuilder();

            Flux<com.openalice.agent.AgentEvent> deltas = agent
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
                    .name(message.role().name().toLowerCase())
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
        return message == null || message.isBlank() ? "agent runtime failed" : message;
    }
}
