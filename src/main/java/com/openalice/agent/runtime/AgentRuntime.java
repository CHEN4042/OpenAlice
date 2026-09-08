package com.openalice.agent.runtime;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import reactor.core.publisher.Flux;

public interface AgentRuntime extends AutoCloseable {

    Flux<AgentEvent> stream(AgentRequest request);

    @Override
    default void close() {
    }
}
