package com.openalice.config;

import com.openalice.agent.runtime.AgentRuntime;
import com.openalice.agent.runtime.AgentRuntimeFactory;
import com.openalice.agent.runtime.AgentRuntimeProperties;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.memory.InMemoryConversationStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root: storage and agent runtime implementations are assembled
 * here so service code depends only on boundaries. External configuration is
 * bound from {@code openalice.*} and mapped onto the agent core's plain
 * {@link AgentRuntimeProperties} value object.
 */
@Configuration
@EnableConfigurationProperties(OpenAliceSettings.class)
public class OpenAliceConfiguration {

    @Bean
    public ConversationStore conversationStore() {
        return new InMemoryConversationStore();
    }

    @Bean
    public AgentRuntimeProperties agentRuntimeProperties(OpenAliceSettings settings) {
        OpenAliceSettings.Agent agent = settings.agent();
        OpenAliceSettings.Llm llm = settings.llm();
        return new AgentRuntimeProperties(
                agent.name(),
                agent.description(),
                agent.systemPrompt(),
                agent.replyPrefix(),
                agent.timeout(),
                agent.workspace(),
                llm.provider(),
                llm.model(),
                llm.baseUrl(),
                llm.proxy(),
                llm.apiKey()
        );
    }

    @Bean(destroyMethod = "close")
    public AgentRuntime agentRuntime(AgentRuntimeProperties properties) {
        return AgentRuntimeFactory.create(properties);
    }
}
