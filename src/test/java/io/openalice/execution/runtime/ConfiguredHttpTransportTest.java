package io.openalice.execution.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class ConfiguredHttpTransportTest {

    @Test
    void configuredHeadersAreAddedAtTheTransportBoundary() {
        AtomicReference<HttpRequest> observed = new AtomicReference<>();
        HttpTransport delegate = new RecordingTransport(observed);
        ConfiguredHttpTransport transport =
                new ConfiguredHttpTransport(delegate, Map.of("X-Provider-Route", "test-route"));

        transport.stream(HttpRequest.builder()
                        .url("https://example.invalid/v1/chat/completions")
                        .method("POST")
                        .headers(Map.of("Content-Type", "application/json"))
                        .body("{}")
                        .build())
                .blockLast();

        assertThat(observed.get().getHeaders())
                .containsEntry("Content-Type", "application/json")
                .containsEntry("X-Provider-Route", "test-route");
    }

    private record RecordingTransport(AtomicReference<HttpRequest> observed)
            implements HttpTransport {
        @Override
        public HttpResponse execute(HttpRequest request) {
            observed.set(request);
            throw new UnsupportedOperationException("Synchronous path is not used by this test");
        }

        @Override
        public Flux<String> stream(HttpRequest request) {
            observed.set(request);
            return Flux.just("done");
        }

        @Override
        public void close() {}
    }
}
