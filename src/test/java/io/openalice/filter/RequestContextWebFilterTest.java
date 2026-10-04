package io.openalice.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.common.log.LogContext;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class RequestContextWebFilterTest {

    private final RequestContextWebFilter filter = new RequestContextWebFilter();

    @Test
    void preservesValidRequestIdAcrossSchedulerSwitchAndReturnsIt() {
        AtomicReference<LogContext> observed = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(RequestContextWebFilter.REQUEST_ID_HEADER, "request-123")
                .build());

        filter.filter(
                        exchange,
                        ignored -> Mono.delay(Duration.ofMillis(1))
                                .publishOn(Schedulers.boundedElastic())
                                .doOnNext(value -> observed.set(LogContext.current()))
                                .then())
                .block(Duration.ofSeconds(2));

        assertThat(exchange.getResponse()
                        .getHeaders()
                        .getFirst(RequestContextWebFilter.REQUEST_ID_HEADER))
                .isEqualTo("request-123");
        assertThat(observed.get().fields()).containsEntry("requestId", "request-123");
    }

    @Test
    void replacesInvalidRequestIdWithGeneratedUuid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(RequestContextWebFilter.REQUEST_ID_HEADER, "invalid request id")
                .build());

        filter.filter(exchange, ignored -> Mono.empty()).block(Duration.ofSeconds(2));

        String requestId =
                exchange.getResponse().getHeaders().getFirst(RequestContextWebFilter.REQUEST_ID_HEADER);
        assertThat(requestId)
                .isNotBlank()
                .isNotEqualTo("invalid request id")
                .matches("[0-9a-f-]{36}");
    }
}
