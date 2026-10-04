package io.openalice.runtime;

import java.util.UUID;
import reactor.core.publisher.Flux;

public interface AgentRuntime {

    Flux<RuntimeEvent> execute(RuntimeRequest request);

    void interrupt(UUID executionId);
}
