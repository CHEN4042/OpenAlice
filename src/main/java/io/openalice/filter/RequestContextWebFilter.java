package io.openalice.filter;

import io.openalice.common.log.LogContext;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextWebFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = requestId(exchange.getRequest().getHeaders());
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        LogContext logContext = LogContext.builder().requestId(requestId).build();
        return chain.filter(exchange).contextWrite(logContext.writeToReactorContext());
    }

    private static String requestId(HttpHeaders headers) {
        String supplied = headers.getFirst(REQUEST_ID_HEADER);
        if (supplied != null && VALID_REQUEST_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }
}
