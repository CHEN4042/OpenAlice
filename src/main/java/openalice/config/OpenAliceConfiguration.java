package openalice.config;

import openalice.agent.runtime.AgentRuntime;
import openalice.agent.runtime.AgentRuntimeFactory;
import openalice.agent.runtime.AgentRuntimeProperties;
import openalice.memory.InMemoryMemoryPort;
import openalice.port.MemoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 组合根：Spring 容器在这里把「存哪（MemoryPort 实现）」和「agent 怎么建」组装好。
 * 以后把 InMemoryMemoryPort 换成 PostgreSQL 实现时，只改这一个类。
 */
@Configuration
public class OpenAliceConfiguration {

    @Bean
    public MemoryPort memoryPort() {
        return new InMemoryMemoryPort();
    }

    @Bean(destroyMethod = "close")
    public AgentRuntime agentRuntime() {
        return AgentRuntimeFactory.create(AgentRuntimeProperties.fromEnvironment());
    }
}
