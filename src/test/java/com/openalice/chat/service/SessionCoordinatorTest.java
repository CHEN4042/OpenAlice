package com.openalice.chat.service;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCoordinatorTest {

    @Test
    void shouldSerializeWorkForSameSession() {
        SessionCoordinator coordinator = new SessionCoordinator();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        Flux<String> first = delayedTask(coordinator, "same", "a", active, maxActive);
        Flux<String> second = delayedTask(coordinator, "same", "b", active, maxActive);

        assertThat(Mono.when(first, second).block(Duration.ofSeconds(5))).isNull();
        assertThat(maxActive.get()).isEqualTo(1);
    }

    @Test
    void shouldRunDifferentSessionsInParallel() {
        SessionCoordinator coordinator = new SessionCoordinator();
        CountDownLatch bothStarted = new CountDownLatch(2);

        Flux<String> first = coordinator.serialize(
                "a",
                Mono.fromRunnable(() -> awaitParallel(bothStarted))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .thenReturn("a")
        );
        Flux<String> second = coordinator.serialize(
                "b",
                Mono.fromRunnable(() -> awaitParallel(bothStarted))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .thenReturn("b")
        );

        var results = Mono.zip(first.next(), second.next()).block(Duration.ofSeconds(2));

        assertThat(results.getT1()).isEqualTo("a");
        assertThat(results.getT2()).isEqualTo("b");
    }

    private static Flux<String> delayedTask(
            SessionCoordinator coordinator,
            String sessionId,
            String value,
            AtomicInteger active,
            AtomicInteger maxActive
    ) {
        return coordinator.serialize(
                sessionId,
                Mono.fromCallable(() -> {
                    int current = active.incrementAndGet();
                    maxActive.accumulateAndGet(current, Math::max);
                    Thread.sleep(100);
                    active.decrementAndGet();
                    return value;
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        );
    }

    private static void awaitParallel(CountDownLatch started) {
        started.countDown();
        try {
            if (!started.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("different sessions were not parallel");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parallel test interrupted", exception);
        }
    }
}
