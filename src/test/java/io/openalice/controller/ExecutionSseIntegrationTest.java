package io.openalice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.common.log.LogWriter;
import io.openalice.common.log.OpenAliceLog;
import io.openalice.common.log.Slf4jLogWriter;
import io.openalice.exception.ErrorCode;
import io.openalice.exception.OpenAliceException;
import io.openalice.mapper.ExecutionMapper;
import io.openalice.model.Execution;
import io.openalice.model.ExecutionStatus;
import io.openalice.runtime.AgentRuntime;
import io.openalice.runtime.RuntimeEvent;
import io.openalice.runtime.RuntimeMessage;
import io.openalice.runtime.RuntimeRequest;
import io.openalice.runtime.RuntimeResult;
import io.openalice.service.ExecutionCoordinator;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
@Import(ExecutionSseIntegrationTest.RuntimeTestConfiguration.class)
class ExecutionSseIntegrationTest {

    @TempDir static Path openAliceHome;

    @DynamicPropertySource
    static void configureHome(DynamicPropertyRegistry registry) {
        registry.add("openalice.home", openAliceHome::toString);
    }

    @LocalServerPort int port;
    @Autowired ExecutionCoordinator coordinator;
    @Autowired ExecutionMapper executionMapper;
    @Autowired ControllableRuntime runtime;
    private final CapturingLogWriter logWriter = new CapturingLogWriter();

    @BeforeEach
    void resetRuntimeEvidence() {
        runtime.reset();
        logWriter.clear();
        OpenAliceLog.configureWriter(logWriter);
    }

    @AfterEach
    void restoreSlf4j() {
        OpenAliceLog.configureWriter(new Slf4jLogWriter());
    }

    @Test
    void disconnectingSseDoesNotCancelRuntimeAndCandidateNeedsProductCommit() {
        UUID executionId = UUID.randomUUID();
        coordinator.start(request(executionId, "continue"));

        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(5))
                .build();
        client.get()
                .uri("/api/executions/{id}/events", executionId)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(ExecutionEventController.EventPayload.class)
                .getResponseBody()
                .take(2)
                .blockLast(Duration.ofSeconds(5));

        RuntimeEvent.CandidateResult candidate = coordinator.events(executionId)
                .ofType(RuntimeEvent.CandidateResult.class)
                .next()
                .block(Duration.ofSeconds(5));

        assertThat(candidate).isNotNull();
        assertThat(runtime.emitted()).isEqualTo(6);
        assertThat(executionMapper.findById(executionId))
                .get()
                .extracting(Execution::status)
                .isEqualTo(ExecutionStatus.RUNNING);

        assertThat(coordinator.completeAfterProductCommit(executionId)).isTrue();
        assertThat(executionMapper.findById(executionId))
                .get()
                .extracting(Execution::status)
                .isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(logWriter.entriesFor("execution.started"))
                .singleElement()
                .satisfies(entry -> assertThat(entry.record().fields())
                        .containsEntry("executionId", executionId.toString())
                        .containsKey("runtime"));
        assertThat(logWriter.entriesFor("execution.completed"))
                .singleElement()
                .satisfies(entry -> assertThat(entry.record().fields())
                        .containsEntry("executionId", executionId.toString()));
    }

    @Test
    void acceptedCancellationPersistsBeforeLateRuntimeCandidate() throws Exception {
        UUID executionId = UUID.randomUUID();
        coordinator.start(request(executionId, "cancel"));

        assertThat(coordinator.cancel(executionId)).isTrue();
        List<RuntimeEvent> delivered =
                coordinator.events(executionId).collectList().block(Duration.ofSeconds(2));
        Thread.sleep(500);

        assertThat(runtime.interrupted()).contains(executionId);
        assertThat(executionMapper.findById(executionId))
                .get()
                .extracting(Execution::status)
                .isEqualTo(ExecutionStatus.CANCELLED);
        assertThat(delivered).anyMatch(RuntimeEvent.Cancelled.class::isInstance);
        assertThat(delivered).noneMatch(RuntimeEvent.CandidateResult.class::isInstance);
        assertThat(delivered).noneMatch(RuntimeEvent.Completed.class::isInstance);
        assertThat(coordinator.completeAfterProductCommit(executionId)).isFalse();
        assertThat(logWriter.entriesFor("execution.cancelled"))
                .singleElement()
                .satisfies(entry -> assertThat(entry.record().fields())
                        .containsEntry("executionId", executionId.toString()));
    }

    @Test
    void missingExecutionUsesOpenAliceNotFoundException() {
        StepVerifier.create(coordinator.events(UUID.randomUUID()))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(OpenAliceException.class, exception ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND)))
                .verify();
    }

    @Test
    void concurrentRuntimeEmissionAndCancellationAreSerialized() {
        for (int attempt = 0; attempt < 25; attempt++) {
            UUID executionId = UUID.randomUUID();
            coordinator.start(request(executionId, "concurrent"));
            CountDownLatch startRace = new CountDownLatch(1);

            CompletableFuture<Void> emission = CompletableFuture.runAsync(() -> {
                await(startRace);
                runtime.emit(
                        executionId,
                        new RuntimeEvent.TextDelta(executionId, Instant.now(), "racing-delta"));
            });
            CompletableFuture<Boolean> cancellation = CompletableFuture.supplyAsync(() -> {
                await(startRace);
                return coordinator.cancel(executionId);
            });

            startRace.countDown();
            emission.join();
            assertThat(cancellation.join()).isTrue();

            List<RuntimeEvent> delivered =
                    coordinator.events(executionId).collectList().block(Duration.ofSeconds(2));
            assertThat(executionMapper.findById(executionId))
                    .get()
                    .extracting(Execution::status)
                    .isEqualTo(ExecutionStatus.CANCELLED);
            assertThat(delivered).anyMatch(RuntimeEvent.Cancelled.class::isInstance);
        }
    }

    private static RuntimeRequest request(UUID executionId, String text) {
        return new RuntimeRequest(
                executionId,
                "test-user",
                "",
                List.of(new RuntimeMessage(RuntimeMessage.Role.USER, text)));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent test", exception);
        }
    }

    @TestConfiguration
    static class RuntimeTestConfiguration {
        @Bean
        @Primary
        ControllableRuntime controllableRuntime() {
            return new ControllableRuntime();
        }
    }

    static final class ControllableRuntime implements AgentRuntime {
        private final AtomicInteger emitted = new AtomicInteger();
        private final Set<UUID> interrupted = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<UUID, FluxSink<RuntimeEvent>> manualSinks =
                new ConcurrentHashMap<>();

        @Override
        public Flux<RuntimeEvent> execute(RuntimeRequest request) {
            if (request.messages().getFirst().text().equals("concurrent")) {
                return Flux.create(sink -> manualSinks.put(request.executionId(), sink));
            }
            Flux<RuntimeEvent> deltas = Flux.interval(Duration.ofMillis(60))
                    .take(6)
                    .map(index -> {
                        emitted.incrementAndGet();
                        return new RuntimeEvent.TextDelta(
                                request.executionId(), Instant.now(), "delta-" + index);
                    });
            return deltas.concatWithValues(new RuntimeEvent.CandidateResult(
                    request.executionId(), Instant.now(), new RuntimeResult("candidate")));
        }

        @Override
        public void interrupt(UUID executionId) {
            interrupted.add(executionId);
        }

        void emit(UUID executionId, RuntimeEvent event) {
            FluxSink<RuntimeEvent> sink = manualSinks.get(executionId);
            if (sink == null) {
                throw new IllegalStateException("Manual runtime is not subscribed");
            }
            sink.next(event);
        }

        int emitted() {
            return emitted.get();
        }

        Set<UUID> interrupted() {
            return Set.copyOf(interrupted);
        }

        void reset() {
            emitted.set(0);
            interrupted.clear();
            manualSinks.clear();
        }
    }

    private static final class CapturingLogWriter implements LogWriter {
        private final List<CapturedLog> entries = new CopyOnWriteArrayList<>();

        @Override
        public void write(Level level, LogRecord record, Throwable error) {
            entries.add(new CapturedLog(level, record, error));
        }

        List<CapturedLog> entriesFor(String event) {
            return entries.stream()
                    .filter(entry -> entry.record().event().equals(event))
                    .toList();
        }

        void clear() {
            entries.clear();
        }
    }

    private record CapturedLog(LogWriter.Level level, LogWriter.LogRecord record, Throwable error) {}
}
