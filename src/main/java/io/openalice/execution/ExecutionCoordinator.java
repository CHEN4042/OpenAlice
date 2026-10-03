package io.openalice.execution;

import io.openalice.execution.runtime.AgentRuntime;
import io.openalice.execution.runtime.RuntimeEvent;
import io.openalice.execution.runtime.RuntimeRequest;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ExecutionCoordinator {

    private static final int IN_MEMORY_EVENT_LIMIT = 64;

    private final ExecutionRepository repository;
    private final AgentRuntime runtime;
    private final Clock clock;
    private final ConcurrentMap<UUID, Sinks.Many<RuntimeEvent>> eventStreams =
            new ConcurrentHashMap<>();

    @Autowired
    public ExecutionCoordinator(ExecutionRepository repository, AgentRuntime runtime) {
        this(repository, runtime, Clock.systemUTC());
    }

    ExecutionCoordinator(ExecutionRepository repository, AgentRuntime runtime, Clock clock) {
        this.repository = repository;
        this.runtime = runtime;
        this.clock = clock;
    }

    public UUID start(RuntimeRequest request) {
        UUID executionId = request.executionId();
        Sinks.Many<RuntimeEvent> events = Sinks.many().replay().limit(IN_MEMORY_EVENT_LIMIT);
        if (eventStreams.putIfAbsent(executionId, events) != null) {
            throw new IllegalStateException("Execution already exists in this process");
        }

        try {
            repository.create(executionId, clock.instant());
        } catch (RuntimeException error) {
            eventStreams.remove(executionId, events);
            throw error;
        }
        emit(events, new RuntimeEvent.Started(executionId, clock.instant()));
        AtomicBoolean terminalSeen = new AtomicBoolean();

        runtime.execute(request).subscribe(
                event -> acceptRuntimeEvent(events, terminalSeen, event),
                error -> fail(events, terminalSeen, executionId, safeMessage(error)),
                () -> {
                    if (!terminalSeen.get()
                            && repository.find(executionId)
                                    .map(Execution::status)
                                    .filter(ExecutionStatus.RUNNING::equals)
                                    .isPresent()) {
                        fail(events, terminalSeen, executionId, "Runtime completed without a result");
                    }
                });
        return executionId;
    }

    public Flux<RuntimeEvent> events(UUID executionId) {
        Sinks.Many<RuntimeEvent> events = eventStreams.get(executionId);
        if (events == null) {
            return Flux.error(new ExecutionNotFoundException(executionId));
        }
        return events.asFlux();
    }

    public boolean cancel(UUID executionId) {
        boolean accepted = repository.transitionFromRunning(
                executionId, ExecutionStatus.CANCELLED, clock.instant());
        if (!accepted) {
            return false;
        }
        runtime.interrupt(executionId);
        Sinks.Many<RuntimeEvent> events = eventStreams.get(executionId);
        if (events != null) {
            emit(events, new RuntimeEvent.Cancelled(executionId, clock.instant()));
            events.tryEmitComplete();
        }
        return true;
    }

    private void acceptRuntimeEvent(
            Sinks.Many<RuntimeEvent> events, AtomicBoolean terminalSeen, RuntimeEvent event) {
        if (event instanceof RuntimeEvent.Completed) {
            terminalSeen.set(true);
            if (repository.transitionFromRunning(
                    event.executionId(), ExecutionStatus.COMPLETED, clock.instant())) {
                emit(events, event);
                events.tryEmitComplete();
            }
        } else if (event instanceof RuntimeEvent.Failed failed) {
            fail(events, terminalSeen, event.executionId(), failed.message());
        } else if (repository.find(event.executionId())
                .map(Execution::status)
                .filter(ExecutionStatus.RUNNING::equals)
                .isPresent()) {
            emit(events, event);
        }
    }

    private void fail(
            Sinks.Many<RuntimeEvent> events,
            AtomicBoolean terminalSeen,
            UUID executionId,
            String message) {
        terminalSeen.set(true);
        if (repository.transitionFromRunning(
                executionId, ExecutionStatus.FAILED, clock.instant())) {
            emit(events, new RuntimeEvent.Failed(executionId, clock.instant(), message));
            events.tryEmitComplete();
        }
    }

    private static void emit(Sinks.Many<RuntimeEvent> sink, RuntimeEvent event) {
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_TERMINATED) {
            throw new IllegalStateException("Unable to publish runtime event: " + result);
        }
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
