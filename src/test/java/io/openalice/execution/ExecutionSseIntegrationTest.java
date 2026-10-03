package io.openalice.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.execution.runtime.AgentRuntime;
import io.openalice.execution.runtime.RuntimeEvent;
import io.openalice.execution.runtime.RuntimeMessage;
import io.openalice.execution.runtime.RuntimeRequest;
import io.openalice.execution.runtime.RuntimeResult;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    @Autowired ExecutionRepository repository;
    @Autowired ControllableRuntime runtime;

    @BeforeEach
    void resetRuntimeEvidence() {
        runtime.reset();
    }

    @Test
    void disconnectingSseDeliveryDoesNotCancelRuntimeExecution() {
        UUID executionId = UUID.randomUUID();
        coordinator.start(new RuntimeRequest(
                executionId,
                "sse-test-user",
                "",
                List.of(new RuntimeMessage(RuntimeMessage.Role.USER, "continue"))));

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

        awaitCompleted(executionId);
        assertThat(runtime.emitted()).isEqualTo(6);
        assertThat(repository.find(executionId)).get().extracting(Execution::status)
                .isEqualTo(ExecutionStatus.COMPLETED);
    }

    @Test
    void acceptedCancellationPersistsBeforeLateRuntimeCompletion() throws Exception {
        UUID executionId = UUID.randomUUID();
        coordinator.start(new RuntimeRequest(
                executionId,
                "cancel-test-user",
                "",
                List.of(new RuntimeMessage(RuntimeMessage.Role.USER, "cancel"))));

        assertThat(coordinator.cancel(executionId)).isTrue();
        List<RuntimeEvent> delivered =
                coordinator.events(executionId).collectList().block(Duration.ofSeconds(2));
        Thread.sleep(500);

        assertThat(runtime.interrupted()).contains(executionId);
        assertThat(repository.find(executionId)).get().extracting(Execution::status)
                .isEqualTo(ExecutionStatus.CANCELLED);
        assertThat(delivered).anyMatch(RuntimeEvent.Cancelled.class::isInstance);
        assertThat(delivered).noneMatch(RuntimeEvent.Completed.class::isInstance);
    }

    private void awaitCompleted(UUID executionId) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (repository.find(executionId)
                    .map(Execution::status)
                    .filter(ExecutionStatus.COMPLETED::equals)
                    .isPresent()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while awaiting runtime completion", exception);
            }
        }
        throw new AssertionError("Runtime did not complete after SSE delivery disconnected");
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

        @Override
        public Flux<RuntimeEvent> execute(RuntimeRequest request) {
            Flux<RuntimeEvent> deltas = Flux.interval(Duration.ofMillis(60))
                    .take(6)
                    .map(index -> {
                        emitted.incrementAndGet();
                        return new RuntimeEvent.TextDelta(
                                request.executionId(), Instant.now(), "delta-" + index);
                    });
            return deltas.concatWithValues(new RuntimeEvent.Completed(
                    request.executionId(), Instant.now(), new RuntimeResult("complete")));
        }

        @Override
        public void interrupt(UUID executionId) {
            interrupted.add(executionId);
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
        }
    }
}
