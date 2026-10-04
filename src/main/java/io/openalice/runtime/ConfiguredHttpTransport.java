package io.openalice.runtime;

import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportException;
import java.util.LinkedHashMap;
import java.util.Map;
import reactor.core.publisher.Flux;

public final class ConfiguredHttpTransport implements HttpTransport {

    private final HttpTransport delegate;
    private final Map<String, String> headers;

    public ConfiguredHttpTransport(HttpTransport delegate, Map<String, String> headers) {
        this.delegate = delegate;
        this.headers = Map.copyOf(headers);
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws HttpTransportException {
        return delegate.execute(withConfiguredHeaders(request));
    }

    @Override
    public Flux<String> stream(HttpRequest request) {
        return delegate.stream(withConfiguredHeaders(request));
    }

    @Override
    public void close() {
        delegate.close();
    }

    private HttpRequest withConfiguredHeaders(HttpRequest request) {
        Map<String, String> merged = new LinkedHashMap<>(request.getHeaders());
        merged.putAll(headers);
        return HttpRequest.builder()
                .url(request.getUrl())
                .method(request.getMethod())
                .headers(merged)
                .body(request.getBody())
                .build();
    }
}
