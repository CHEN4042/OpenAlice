package com.openalice.config;

import com.openalice.agent.runtime.AgentRuntime;
import com.openalice.agent.runtime.AgentRuntimeFactory;
import com.openalice.agent.runtime.AgentRuntimeProperties;
import com.openalice.repository.ConversationStore;
import com.openalice.repository.memory.InMemoryConversationStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root: storage and agent runtime implementations are assembled
 * here so service code depends only on boundaries.
 */
@Configuration
public class OpenAliceConfiguration {

    @Bean
    public ConversationStore conversationStore() {
        return new InMemoryConversationStore();
    }

    @Bean
    public AgentRuntimeProperties agentRuntimeProperties() {
        return AgentRuntimeProperties.fromEnvironment();
    }

    @Bean(destroyMethod = "close")
    public AgentRuntime agentRuntime(AgentRuntimeProperties properties) {
        return AgentRuntimeFactory.create(properties);
    }
}
