package openalice.agent.runtime;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import java.util.Objects;
import openalice.agent.config.AgentRuntimeProperties;
import openalice.agent.model.LlmModelFactory;
import openalice.core.domain.ChatMessage;
import openalice.core.domain.MessageRole;
import openalice.core.port.MemoryPort;

final class AgentScopeAgentRuntime implements AgentRuntime {

    private final MemoryPort memoryPort;
    private final HarnessAgent agent;
    private final Duration timeout;

    AgentScopeAgentRuntime(MemoryPort memoryPort, AgentRuntimeProperties properties) {
        this.memoryPort = Objects.requireNonNull(memoryPort, "memoryPort must not be null");
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
    public ChatResult chat(ChatMessage userMessage) {
        validate(userMessage);
        RuntimeContext context = RuntimeContext.builder()
                .userId(userMessage.userId().value())
                .sessionId(userMessage.sessionId().value())
                .build();

        memoryPort.append(userMessage);
        Msg response = agent.call(userMessage.content(), context).block(timeout);
        if (response == null) {
            throw new IllegalStateException("AgentScope returned no response");
        }
        String reply = response.getTextContent();
        memoryPort.append(ChatMessage.assistant(
                userMessage.userId(),
                userMessage.sessionId(),
                reply
        ));
        return new ChatResult(userMessage.userId(), userMessage.sessionId(), reply);
    }

    @Override
    public void close() {
        agent.close();
    }

    private static void validate(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("userMessage must not be null");
        }
        if (message.role() != MessageRole.USER) {
            throw new IllegalArgumentException("agent chat only accepts USER messages");
        }
    }
}
