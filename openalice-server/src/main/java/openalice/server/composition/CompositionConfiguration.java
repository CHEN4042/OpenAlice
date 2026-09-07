package openalice.server.composition;

import openalice.agent.config.AgentRuntimeProperties;
import openalice.agent.runtime.AgentRuntime;
import openalice.agent.runtime.AgentRuntimeFactory;
import openalice.core.port.MemoryPort;
import openalice.memory.store.inmemory.InMemoryMemoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CompositionConfiguration {

    @Bean
    MemoryPort memoryPort() {
        return new InMemoryMemoryPort();
    }

    @Bean(destroyMethod = "close")
    AgentRuntime agentRuntime(MemoryPort memoryPort) {
        return AgentRuntimeFactory.create(memoryPort, AgentRuntimeProperties.fromEnvironment());
    }
}

