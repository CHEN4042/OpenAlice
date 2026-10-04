package io.openalice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.model.Execution;
import io.openalice.model.ExecutionStatus;
import io.openalice.service.ExecutionStartupReconciler;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.main.banner-mode=off")
class ExecutionMapperIntegrationTest {

    @TempDir static Path openAliceHome;

    @DynamicPropertySource
    static void configureHome(DynamicPropertyRegistry registry) {
        registry.add("openalice.home", openAliceHome::toString);
    }

    @Autowired ExecutionMapper executionMapper;
    @Autowired ExecutionStartupReconciler startupReconciler;

    @Test
    void mapperInsertsReadsAndConditionallyTransitionsExecution() {
        UUID executionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-04T00:00:00Z");
        Execution execution =
                new Execution(executionId, ExecutionStatus.RUNNING, createdAt, createdAt);

        assertThat(executionMapper.insert(execution)).isOne();
        assertThat(executionMapper.findById(executionId)).contains(execution);

        Instant completedAt = createdAt.plusSeconds(5);
        assertThat(executionMapper.transitionFromRunning(
                        executionId, ExecutionStatus.COMPLETED, completedAt))
                .isOne();
        assertThat(executionMapper.transitionFromRunning(
                        executionId, ExecutionStatus.FAILED, completedAt.plusSeconds(1)))
                .isZero();
        assertThat(executionMapper.findById(executionId))
                .get()
                .extracting(Execution::status, Execution::updatedAt)
                .containsExactly(ExecutionStatus.COMPLETED, completedAt);
    }

    @Test
    void startupReconciliationInterruptsStaleRunningExecutions() {
        UUID executionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-03T23:59:00Z");
        executionMapper.insert(
                new Execution(executionId, ExecutionStatus.RUNNING, createdAt, createdAt));

        assertThat(startupReconciler.reconcile()).isOne();

        assertThat(executionMapper.findById(executionId))
                .get()
                .extracting(Execution::status)
                .isEqualTo(ExecutionStatus.INTERRUPTED);
    }
}
