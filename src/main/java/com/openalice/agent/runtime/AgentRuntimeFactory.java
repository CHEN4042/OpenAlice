package com.openalice.agent.runtime;

/**
 * agent 域的构造入口：隐藏 AgentScope 实现类，统一从这里创建 {@link AgentRuntime}。
 * Spring 组合配置（{@code com.openalice.config.OpenAliceConfiguration}）调用它。
 */
public final class AgentRuntimeFactory {

    private AgentRuntimeFactory() {
    }

    public static AgentRuntime create(AgentRuntimeProperties properties) {
        return new AgentScopeAgentRuntime(properties);
    }
}
