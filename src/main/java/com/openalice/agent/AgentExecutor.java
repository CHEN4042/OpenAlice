package com.openalice.agent;

import reactor.core.publisher.Flux;

/**
 * Agent 执行端口：把「执行一轮 agent 并产出事件流」的职责收口在这里。
 *
 * <p>上层（chat.service）只依赖本接口，具体实现（目前是基于 AgentScope ReAct 引擎的
 * {@link AgentScopeReActAgent}）由组合根注入，便于替换与测试。</p>
 */
public interface AgentExecutor extends AutoCloseable {

    Flux<AgentEvent> stream(AgentRequest request);

    @Override
    default void close() {
    }
}
