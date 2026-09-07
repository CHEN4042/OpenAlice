package com.openalice.agent.runtime;

import com.openalice.agent.AgentEvent;
import com.openalice.agent.AgentRequest;
import com.openalice.agent.DoneEvent;
import com.openalice.model.SessionId;
import com.openalice.model.UserId;
import java.util.List;
import reactor.core.publisher.Flux;

public interface AgentRuntime extends AutoCloseable {

    Flux<AgentEvent> stream(AgentRequest request);

    /** Blocking convenience method for callers that do not need token streaming. */
    default ChatResult chat(AgentRequest request) {
        List<AgentEvent> events = stream(request).collectList().block();
        String reply = events == null ? null : events.stream()
                .filter(DoneEvent.class::isInstance)
                .map(DoneEvent.class::cast)
                .map(DoneEvent::reply)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("agent stream did not complete"));
        return new ChatResult(
                request.turn().userId(),
                request.turn().sessionId(),
                reply
        );
    }

    @Override
    default void close() {
    }
}
