package io.openalice.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class OpenAliceLogTest {

    private final CapturingLogWriter writer = new CapturingLogWriter();

    @BeforeEach
    void captureLogs() {
        OpenAliceLog.configureWriter(writer);
    }

    @AfterEach
    void restoreSlf4j() {
        OpenAliceLog.configureWriter(new Slf4jLogWriter());
    }

    @Test
    void builderCombinesEventMessageDynamicFieldsAndCommonContext() {
        LogContext context = LogContext.builder()
                .requestId("request-1")
                .executionId("execution-1")
                .field("experimentId", "experiment-a")
                .build();

        context.run(() -> OpenAliceLog.event("execution.started")
                .message("Execution started")
                .field("model", "test-model")
                .fields(Map.of("tokenUsage", 7))
                .info());

        CapturedLog captured = writer.single();
        assertThat(captured.level()).isEqualTo(LogWriter.Level.INFO);
        assertThat(captured.record().event()).isEqualTo("execution.started");
        assertThat(captured.record().message()).isEqualTo("Execution started");
        assertThat(captured.record().fields())
                .containsEntry("requestId", "request-1")
                .containsEntry("executionId", "execution-1")
                .containsEntry("experimentId", "experiment-a")
                .containsEntry("model", "test-model")
                .containsEntry("tokenUsage", 7);
    }

    @Test
    void reactorContextSurvivesSchedulerSwitchForSynchronousLogApi() {
        String callingThread = Thread.currentThread().getName();
        LogContext context = LogContext.builder().requestId("reactive-request").build();

        Mono.just("value")
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(ignored -> OpenAliceLog.event("request.completed")
                        .message("Reactive request completed")
                        .field("thread", Thread.currentThread().getName())
                        .info())
                .contextWrite(context.writeToReactorContext())
                .block();

        CapturedLog captured = writer.single();
        assertThat(captured.record().fields())
                .containsEntry("requestId", "reactive-request")
                .doesNotContainEntry("thread", callingThread);
        assertThat(LogContext.current().fields()).isEmpty();
    }

    @Test
    void rejectsUnstableEventNamesAndSensitivePayloadFields() {
        assertThatThrownBy(() -> OpenAliceLog.event("ExecutionCoordinator.start"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpenAliceLog.event("model.request").field("prompt", "secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class CapturingLogWriter implements LogWriter {
        private final List<CapturedLog> entries = new ArrayList<>();

        @Override
        public synchronized void write(Level level, LogRecord record, Throwable error) {
            entries.add(new CapturedLog(level, record, error));
        }

        synchronized CapturedLog single() {
            assertThat(entries).hasSize(1);
            return entries.getFirst();
        }
    }

    private record CapturedLog(LogWriter.Level level, LogWriter.LogRecord record, Throwable error) {}
}
