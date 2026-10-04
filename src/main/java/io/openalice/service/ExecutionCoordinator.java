package io.openalice.service;

import io.openalice.common.log.LogContext;
import io.openalice.common.log.OpenAliceLog;
import io.openalice.exception.ErrorCode;
import io.openalice.exception.OpenAliceException;
import io.openalice.mapper.ExecutionMapper;
import io.openalice.model.Execution;
import io.openalice.model.ExecutionStatus;
import io.openalice.runtime.AgentRuntime;
import io.openalice.runtime.RuntimeEvent;
import io.openalice.runtime.RuntimeRequest;
import io.openalice.runtime.RuntimeResult;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ExecutionCoordinator {

    private static final int IN_MEMORY_EVENT_LIMIT = 64;

    private final ExecutionMapper executionMapper;
    private final AgentRuntime runtime;
    private final Clock clock;
    private final ConcurrentMap<UUID, ExecutionStream> eventStreams = new ConcurrentHashMap<>();

    @Autowired
    public ExecutionCoordinator(ExecutionMapper executionMapper, AgentRuntime runtime) {
        this(executionMapper, runtime, Clock.systemUTC());
    }

    ExecutionCoordinator(ExecutionMapper executionMapper, AgentRuntime runtime, Clock clock) {
        this.executionMapper = executionMapper;
        this.runtime = runtime;
        this.clock = clock;
    }

    public UUID start(RuntimeRequest request) {
        UUID executionId = request.executionId();
        LogContext executionContext =
                LogContext.current().withExecutionId(executionId.toString());
        ExecutionStream stream = new ExecutionStream(executionContext);
        if (eventStreams.putIfAbsent(executionId, stream) != null) {
            throw new IllegalStateException("Execution already exists in this process");
        }

        try {
            var now = clock.instant();
            if (executionMapper.insert(
                            new Execution(executionId, ExecutionStatus.RUNNING, now, now))
                    != 1) {
                throw new IllegalStateException("Unable to persist Execution");
            }
        } catch (RuntimeException error) {
            eventStreams.remove(executionId, stream);
            throw error;
        }
        stream.publish(new RuntimeEvent.Started(executionId, clock.instant()));
        stream.log(() -> OpenAliceLog.event("execution.started")
                .message("Execution started")
                .field("runtime", runtime.getClass().getSimpleName())
                .info());

        runtime.execute(request)
                .doOnNext(event -> acceptRuntimeEvent(stream, event))
                .doOnError(error -> fail(stream, executionId, safeMessage(error)))
                .doOnComplete(() -> finishRuntime(stream, executionId))
                .contextWrite(executionContext.writeToReactorContext())
                .subscribe(ignored -> {}, ignored -> {});
        return executionId;
    }

    public Flux<RuntimeEvent> events(UUID executionId) {
        ExecutionStream stream = eventStreams.get(executionId);
        if (stream == null) {
            return Flux.error(new OpenAliceException(
                    ErrorCode.NOT_FOUND,
                    "Execution event stream not found: " + executionId));
        }
        return stream.events();
    }

    /**
     * Marks an Execution complete only after the product layer has durably committed the visible
     * result. Runtime candidate production alone never calls this gate.
     */
    public boolean completeAfterProductCommit(UUID executionId) {
        ExecutionStream stream = eventStreams.get(executionId);
        if (stream == null) {
            return false;
        }
        synchronized (stream) {
            RuntimeResult candidate = stream.candidateResult();
            if (candidate == null
                    || executionMapper.transitionFromRunning(
                                    executionId, ExecutionStatus.COMPLETED, clock.instant())
                            != 1) {
                return false;
            }
            stream.publishLocked(
                    new RuntimeEvent.Completed(executionId, clock.instant(), candidate));
            stream.completeLocked();
            stream.log(() -> OpenAliceLog.event("execution.completed")
                    .message("Execution completed after product commit")
                    .info());
            return true;
        }
    }

    public boolean cancel(UUID executionId) {
        ExecutionStream stream = eventStreams.get(executionId);
        boolean accepted;
        if (stream == null) {
            accepted = executionMapper.transitionFromRunning(
                            executionId, ExecutionStatus.CANCELLED, clock.instant())
                    == 1;
        } else {
            synchronized (stream) {
                accepted = executionMapper.transitionFromRunning(
                                executionId, ExecutionStatus.CANCELLED, clock.instant())
                        == 1;
                if (accepted) {
                    stream.publishLocked(
                            new RuntimeEvent.Cancelled(executionId, clock.instant()));
                    stream.completeLocked();
                    stream.log(() -> OpenAliceLog.event("execution.cancelled")
                            .message("Execution cancelled")
                            .info());
                }
            }
        }
        if (accepted) {
            runtime.interrupt(executionId);
        }
        return accepted;
    }

    private void acceptRuntimeEvent(ExecutionStream stream, RuntimeEvent event) {
        if (event instanceof RuntimeEvent.Failed failed) {
            fail(stream, event.executionId(), failed.message());
            return;
        }

        synchronized (stream) {
            if (!isRunning(event.executionId())) {
                return;
            }
            if (event instanceof RuntimeEvent.CandidateResult candidate) {
                if (stream.candidateResult() == null) {
                    stream.setCandidateResult(candidate.result());
                    stream.publishLocked(candidate);
                }
            } else if (event instanceof RuntimeEvent.TextDelta) {
                stream.publishLocked(event);
            } else {
                failLocked(
                        stream,
                        event.executionId(),
                        "Runtime emitted an event owned by Execution coordination");
            }
        }
    }

    private void finishRuntime(ExecutionStream stream, UUID executionId) {
        synchronized (stream) {
            if (stream.candidateResult() == null && isRunning(executionId)) {
                failLocked(stream, executionId, "Runtime completed without a candidate result");
            }
        }
    }

    private void fail(ExecutionStream stream, UUID executionId, String message) {
        synchronized (stream) {
            failLocked(stream, executionId, message);
        }
    }

    private void failLocked(ExecutionStream stream, UUID executionId, String message) {
        if (executionMapper.transitionFromRunning(
                        executionId, ExecutionStatus.FAILED, clock.instant())
                == 1) {
            stream.publishLocked(
                    new RuntimeEvent.Failed(executionId, clock.instant(), message));
            stream.completeLocked();
            stream.log(() -> OpenAliceLog.event("execution.failed")
                    .message("Execution failed")
                    .error());
        }
    }

    private boolean isRunning(UUID executionId) {
        return executionMapper
                .findById(executionId)
                .map(Execution::status)
                .filter(ExecutionStatus.RUNNING::equals)
                .isPresent();
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static final class ExecutionStream {
        private final Sinks.Many<RuntimeEvent> sink =
                Sinks.many().replay().limit(IN_MEMORY_EVENT_LIMIT);
        private final LogContext logContext;
        private RuntimeResult candidateResult;

        private ExecutionStream(LogContext logContext) {
            this.logContext = logContext;
        }

        Flux<RuntimeEvent> events() {
            return sink.asFlux();
        }

        synchronized void publish(RuntimeEvent event) {
            publishLocked(event);
        }

        void publishLocked(RuntimeEvent event) {
            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (result.isFailure() && result != Sinks.EmitResult.FAIL_TERMINATED) {
                throw new IllegalStateException("Unable to publish Execution event: " + result);
            }
        }

        void completeLocked() {
            Sinks.EmitResult result = sink.tryEmitComplete();
            if (result.isFailure() && result != Sinks.EmitResult.FAIL_TERMINATED) {
                throw new IllegalStateException("Unable to complete Execution event stream: " + result);
            }
        }

        RuntimeResult candidateResult() {
            return candidateResult;
        }

        void setCandidateResult(RuntimeResult candidateResult) {
            this.candidateResult = candidateResult;
        }

        void log(Runnable logAction) {
            logContext.run(logAction);
        }
    }
}
