package com.openalice.llm;

import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.HttpTransportException;
import io.agentscope.core.model.transport.HttpTransportFactory;
import io.agentscope.core.model.transport.OkHttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reactor.core.publisher.Flux;

/**
 * {@link HttpTransport} 装饰器：按需走 HTTP 代理，以及为 AgentRouter 中转的
 * WAF 注入 Codex CLI 指纹请求头。
 *
 * 该中转只放行看起来来自官方 Codex 客户端的请求（校验
 * {@code Originator} / {@code Version} / {@code User-Agent}）；底层
 * {@code OpenAIClient} 只会发送 {@code Authorization} / {@code Content-Type}
 * 和它自己的 {@code User-Agent}，否则这类请求会被 401 拒绝。
 *
 * 生命周期：这里创建的带代理 transport 会注册到 {@link HttpTransportFactory}，
 * 由它统一在关闭时回收；共享的默认 transport 不会被本装饰器关闭。
 */
final class ConfiguredHttpTransport implements HttpTransport {

    private static final Pattern HOST_PORT = Pattern.compile("^(https?://)?([^:/]+):(\\d{1,5})$");

    private static final Map<String, String> CODEX_CLIENT_HEADERS = Map.of(
            "Originator", "codex_cli_rs",
            "User-Agent", "codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464",
            "Version", "0.101.0"
    );

    private final HttpTransport delegate;
    private final Map<String, String> extraHeaders;

    private ConfiguredHttpTransport(HttpTransport delegate, Map<String, String> extraHeaders) {
        this.delegate = delegate;
        this.extraHeaders = extraHeaders;
    }

    static ConfiguredHttpTransport create(String proxy, boolean codexClientFingerprint) {
        HttpTransport delegate = proxy == null
                ? HttpTransportFactory.getDefault()
                : proxied(proxy);
        Map<String, String> headers = codexClientFingerprint ? CODEX_CLIENT_HEADERS : Map.of();
        return new ConfiguredHttpTransport(delegate, headers);
    }

    private static HttpTransport proxied(String proxy) {
        ProxyHostPort parsed = parse(proxy);
        HttpTransportConfig config = HttpTransportConfig.builder()
                .proxy(ProxyConfig.http(parsed.host(), parsed.port()))
                .build();
        HttpTransport transport = new OkHttpTransport.Builder().config(config).build();
        HttpTransportFactory.register(transport);
        return transport;
    }

    private static ProxyHostPort parse(String value) {
        Matcher matcher = HOST_PORT.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid proxy '" + value + "', expected host:port such as 127.0.0.1:7897"
            );
        }
        return new ProxyHostPort(matcher.group(2), Integer.parseInt(matcher.group(3)));
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws HttpTransportException {
        return delegate.execute(withHeaders(request));
    }

    @Override
    public Flux<String> stream(HttpRequest request) {
        return delegate.stream(withHeaders(request));
    }

    @Override
    public void close() {
        // 自建的 transport 已注册到 HttpTransportFactory 统一管理生命周期；
        // 共享默认 transport 不能在这里关闭。
    }

    private HttpRequest withHeaders(HttpRequest request) {
        if (extraHeaders.isEmpty()) {
            return request;
        }
        Map<String, String> headers = new LinkedHashMap<>(request.getHeaders());
        headers.putAll(extraHeaders);
        return HttpRequest.builder()
                .url(request.getUrl())
                .method(request.getMethod())
                .headers(headers)
                .body(request.getBody())
                .build();
    }

    private record ProxyHostPort(String host, int port) {
    }
}
