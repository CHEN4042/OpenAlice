package openalice.agent.runtime;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import java.util.Objects;
import openalice.agent.llm.LlmModelFactory;
import openalice.model.ChatMessage;
import openalice.enums.MessageRole;

/**
 * AgentScope 底座上的 agent 执行器。
 *
 * <p>职责只做一件事：把一条 USER 消息交给 AgentScope HarnessAgent，拿回一句回复。
 * 「存消息」这类编排不再在这里做，已上移到 {@code openalice.service.ChatService}。</p>
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
    public ChatResult chat(ChatMessage userMessage) {
        validate(userMessage);
        RuntimeContext context = RuntimeContext.builder()
                .userId(userMessage.userId().value())
                .sessionId(userMessage.sessionId().value())
                .build();

        Msg response = agent.call(userMessage.content(), context).block(timeout);
        if (response == null) {
            throw new IllegalStateException("AgentScope returned no response");
        }
        return new ChatResult(
                userMessage.userId(),
                userMessage.sessionId(),
                response.getTextContent()
        );
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
