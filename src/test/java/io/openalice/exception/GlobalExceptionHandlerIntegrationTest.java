package io.openalice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.common.log.LogWriter;
import io.openalice.common.log.OpenAliceLog;
import io.openalice.common.log.Slf4jLogWriter;
import io.openalice.filter.RequestContextWebFilter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "openalice.home=${java.io.tmpdir}/openalice-exception-${random.uuid}",
            "spring.main.banner-mode=off"
        })
@Import(GlobalExceptionHandlerIntegrationTest.FailureController.class)
class GlobalExceptionHandlerIntegrationTest {

    @LocalServerPort int port;

    private final CapturingLogWriter writer = new CapturingLogWriter();
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        writer.clear();
        OpenAliceLog.configureWriter(writer);
        client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void restoreSlf4j() {
        OpenAliceLog.configureWriter(new Slf4jLogWriter());
    }

    @Test
    void openAliceExceptionUsesMappedStatusWithoutStacktraceLog() {
        client.get()
                .uri("/test/expected")
                .header(RequestContextWebFilter.REQUEST_ID_HEADER, "expected-request")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .valueEquals(RequestContextWebFilter.REQUEST_ID_HEADER, "expected-request")
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("not_found")
                .jsonPath("$.detail")
                .isEqualTo("Test resource not found");

        CapturedLog captured = writer.single();
        assertThat(captured.level()).isEqualTo(LogWriter.Level.WARN);
        assertThat(captured.error()).isNull();
        assertThat(captured.record().fields())
                .containsEntry("requestId", "expected-request")
                .containsEntry("errorCode", "not_found");
    }

    @Test
    void unexpectedExceptionReturns500AndUsesUnifiedErrorLog() {
        client.get()
                .uri("/test/unexpected")
                .header(RequestContextWebFilter.REQUEST_ID_HEADER, "unexpected-request")
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectHeader()
                .valueEquals(RequestContextWebFilter.REQUEST_ID_HEADER, "unexpected-request")
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("internal_error")
                .jsonPath("$.detail")
                .isEqualTo("Internal server error");

        CapturedLog captured = writer.single();
        assertThat(captured.level()).isEqualTo(LogWriter.Level.ERROR);
        assertThat(captured.error()).isInstanceOf(IllegalStateException.class);
        assertThat(captured.record().fields())
                .containsEntry("requestId", "unexpected-request")
                .containsEntry("errorCode", "internal_error");
    }

    @RestController
    static class FailureController {

        @GetMapping("/test/expected")
        String expected() {
            throw new OpenAliceException(ErrorCode.NOT_FOUND, "Test resource not found");
        }

        @GetMapping("/test/unexpected")
        Mono<String> unexpected() {
            return Mono.just("trigger")
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(ignored -> Mono.error(new IllegalStateException("test failure")));
        }
    }

    private static final class CapturingLogWriter implements LogWriter {
        private final List<CapturedLog> entries = new CopyOnWriteArrayList<>();

        @Override
        public void write(Level level, LogRecord record, Throwable error) {
            entries.add(new CapturedLog(level, record, error));
        }

        CapturedLog single() {
            assertThat(entries).hasSize(1);
            return entries.getFirst();
        }

        void clear() {
            entries.clear();
        }
    }

    private record CapturedLog(LogWriter.Level level, LogWriter.LogRecord record, Throwable error) {}
}
