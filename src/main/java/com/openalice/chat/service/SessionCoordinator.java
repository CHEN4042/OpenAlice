package com.openalice.chat.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 按会话串行化执行，同时允许不同会话并行。
 *
 * <p>信号量在整条流式生命周期内持有——覆盖 USER 追加、历史读取、agent 执行与
 * ASSISTANT 追加，保证同一会话的消息顺序不被并发请求打乱。</p>
 */
@Service
public class SessionCoordinator {

    private final Map<String, Semaphore> sessions = new ConcurrentHashMap<>();

    public <T> Flux<T> serialize(String sessionId, Mono<T> action) {
        return serialize(sessionId, action.flux());
    }

    public <T> Flux<T> serialize(String sessionId, Publisher<T> action) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Semaphore semaphore = sessions.computeIfAbsent(sessionId, key -> new Semaphore(1));
        return Flux.usingWhen(
                Mono.fromRunnable(semaphore::acquireUninterruptibly)
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(semaphore),
                acquired -> Flux.from(action),
                acquired -> release(semaphore),
                (acquired, error) -> release(semaphore),
                acquired -> release(semaphore)
        );
    }

    private static Mono<Void> release(Semaphore semaphore) {
        return Mono.fromRunnable(semaphore::release);
    }
}
