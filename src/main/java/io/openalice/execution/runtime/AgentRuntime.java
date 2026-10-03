package io.openalice.execution.runtime;

import java.util.UUID;
import reactor.core.publisher.Flux;

public interface AgentRuntime {

    Flux<RuntimeEvent> execute(RuntimeRequest request);

    void interrupt(UUID executionId);
}
