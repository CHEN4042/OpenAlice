package com.openalice.config;

import com.openalice.agent.AgentExecutor;
import com.openalice.agent.AgentScopeReActAgent;
import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.memory.InMemoryConversationStore;
import com.openalice.chat.store.postgres.PostgresConversationStore;
import com.openalice.llm.LlmSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * 组合根：存储与 Agent 的具体实现都在这里装配，service 只依赖端口接口。
 * 外部配置从 {@code openalice.*} 绑定后，拆成 agent 行为参数与
 * {@link LlmSettings}（普通值对象），供 agent 核心消费。
 */
@Configuration
@EnableConfigurationProperties(OpenAliceSettings.class)
public class OpenAliceConfiguration {

    @Bean
    @ConditionalOnProperty(name = "openalice.store.type", havingValue = "memory")
    public ConversationStore inMemoryConversationStore() {
        return new InMemoryConversationStore();
    }

    @Bean
    @ConditionalOnProperty(name = "openalice.store.type", havingValue = "postgres", matchIfMissing = true)
    public ConversationStore postgresConversationStore(JdbcClient jdbcClient) {
        return new PostgresConversationStore(jdbcClient);
    }

    @Bean(destroyMethod = "close")
    public AgentExecutor agentExecutor(OpenAliceSettings settings) {
        OpenAliceSettings.Agent agent = settings.agent();
        OpenAliceSettings.Llm llm = settings.llm();
        return new AgentScopeReActAgent(
                agent.name(),
                agent.description(),
                agent.systemPrompt(),
                agent.timeout(),
                new LlmSettings(llm.provider(), llm.model(), llm.baseUrl(), llm.proxy(), llm.apiKey())
        );
    }
}
