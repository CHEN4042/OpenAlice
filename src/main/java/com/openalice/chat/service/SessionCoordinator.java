package com.openalice.chat.service;

import com.openalice.model.SessionId;
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
 * Serializes work by session while allowing different sessions to proceed in
 * parallel. The permit is held for the whole streaming lifecycle, including
 * USER append, history read, agent execution, and ASSISTANT append.
 */
@Service
public class SessionCoordinator {

    private final Map<SessionId, Semaphore> sessions = new ConcurrentHashMap<>();

    public <T> Flux<T> serialize(SessionId sessionId, Mono<T> action) {
        return serialize(sessionId, action.flux());
    }

    public <T> Flux<T> serialize(SessionId sessionId, Publisher<T> action) {
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
