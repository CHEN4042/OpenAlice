package openalice.agent.runtime;

import openalice.agent.config.AgentRuntimeProperties;
import openalice.core.port.MemoryPort;

public final class AgentRuntimeFactory {

    private AgentRuntimeFactory() {
    }

    public static AgentRuntime create(MemoryPort memoryPort) {
        return create(memoryPort, AgentRuntimeProperties.defaults());
    }

    public static AgentRuntime create(MemoryPort memoryPort, AgentRuntimeProperties properties) {
        return new AgentScopeAgentRuntime(memoryPort, properties);
    }
}

