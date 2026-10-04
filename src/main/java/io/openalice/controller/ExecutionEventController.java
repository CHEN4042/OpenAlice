package io.openalice.controller;

import io.openalice.runtime.RuntimeEvent;
import io.openalice.service.ExecutionCoordinator;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/executions")
public class ExecutionEventController {

    private final ExecutionCoordinator coordinator;

    public ExecutionEventController(ExecutionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping(value = "/{executionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventPayload>> events(@PathVariable UUID executionId) {
        return coordinator.events(executionId).map(this::toServerSentEvent);
    }

    private ServerSentEvent<EventPayload> toServerSentEvent(RuntimeEvent event) {
        String type;
        String text = null;
        if (event instanceof RuntimeEvent.Started) {
            type = "started";
        } else if (event instanceof RuntimeEvent.TextDelta delta) {
            type = "text-delta";
            text = delta.delta();
        } else if (event instanceof RuntimeEvent.CandidateResult candidate) {
            type = "candidate-result";
            text = candidate.result().text();
        } else if (event instanceof RuntimeEvent.Completed completed) {
            type = "completed";
            text = completed.result().text();
        } else if (event instanceof RuntimeEvent.Failed failed) {
            type = "failed";
            text = failed.message();
        } else {
            type = "cancelled";
        }
        return ServerSentEvent.<EventPayload>builder()
                .event(type)
                .data(new EventPayload(event.executionId(), event.occurredAt(), type, text))
                .build();
    }

    public record EventPayload(UUID executionId, Instant occurredAt, String type, String text) {}
}
