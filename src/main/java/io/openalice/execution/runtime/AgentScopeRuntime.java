package io.openalice.execution.runtime;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AgentScopeRuntime implements AgentRuntime {

    private final AgentModelFactory modelFactory;
    private final Clock clock;
    private final ConcurrentMap<UUID, ActiveAgent> activeAgents = new ConcurrentHashMap<>();

    @Autowired
    public AgentScopeRuntime(AgentModelFactory modelFactory) {
        this(modelFactory, Clock.systemUTC());
    }

    AgentScopeRuntime(AgentModelFactory modelFactory, Clock clock) {
        this.modelFactory = modelFactory;
        this.clock = clock;
    }

    @Override
    public Flux<RuntimeEvent> execute(RuntimeRequest request) {
        return Flux.defer(() -> executeIsolated(request));
    }

    @Override
    public void interrupt(UUID executionId) {
        ActiveAgent active = activeAgents.get(executionId);
        if (active != null) {
            active.agent().interrupt(active.context());
        }
    }

    private Flux<RuntimeEvent> executeIsolated(RuntimeRequest request) {
        AgentModelConnection connection = modelFactory.create();
        ReActAgent agent = ReActAgent.builder()
                .name("openalice")
                .description("OpenAlice execution-scoped runtime using model alias "
                        + connection.alias())
                .sysPrompt(request.systemPrompt())
                .model(connection.model())
                .stateStore(new InMemoryAgentStateStore())
                .defaultSessionId(request.executionId().toString())
                .build();
        RuntimeContext context = RuntimeContext.builder()
                .userId(request.userId())
                .sessionId(request.executionId().toString())
                .build();
        ActiveAgent active = new ActiveAgent(agent, context, connection);
        if (activeAgents.putIfAbsent(request.executionId(), active) != null) {
            close(active);
            return Flux.error(new IllegalStateException("Execution is already active"));
        }

        List<Msg> messages = request.messages().stream().map(this::toAgentScopeMessage).toList();
        return agent.streamEvents(messages, context)
                .<RuntimeEvent>handle(
                        (event, sink) -> mapEvent(request.executionId(), event, sink))
                .onErrorResume(error -> Flux.just(new RuntimeEvent.Failed(
                        request.executionId(), clock.instant(), safeMessage(error))))
                .doFinally(signal -> {
                    activeAgents.remove(request.executionId(), active);
                    close(active);
                });
    }

    private Msg toAgentScopeMessage(RuntimeMessage message) {
        MsgRole role = message.role() == RuntimeMessage.Role.USER
                ? MsgRole.USER
                : MsgRole.ASSISTANT;
        return Msg.builder()
                .name(message.role().name().toLowerCase())
                .role(role)
                .textContent(message.text())
                .build();
    }

    private void mapEvent(
            UUID executionId,
            AgentEvent event,
            reactor.core.publisher.SynchronousSink<RuntimeEvent> sink) {
        Instant now = clock.instant();
        if (event instanceof TextBlockDeltaEvent delta && !delta.getDelta().isEmpty()) {
            sink.next(new RuntimeEvent.TextDelta(executionId, now, delta.getDelta()));
        } else if (event instanceof AgentResultEvent result && result.getResult() != null) {
            sink.next(new RuntimeEvent.Completed(
                    executionId, now, new RuntimeResult(result.getResult().getTextContent())));
        }
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static void close(ActiveAgent active) {
        try {
            active.agent().close();
        } finally {
            try {
                active.connection().close();
            } catch (Exception exception) {
                // Runtime resources are best-effort cleanup after the execution has terminated.
            }
        }
    }

    private record ActiveAgent(
            ReActAgent agent, RuntimeContext context, AgentModelConnection connection) {}
}
