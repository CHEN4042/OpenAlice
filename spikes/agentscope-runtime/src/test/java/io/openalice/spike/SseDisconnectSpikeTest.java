package io.openalice.spike;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@SpringBootTest(
        classes = SseDisconnectSpikeTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.main.banner-mode=off",
            "logging.level.root=WARN",
            "server.tomcat.threads.max=20"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SseDisconnectSpikeTest {

    @LocalServerPort int port;

    @BeforeEach
    void reset() {
        Probe.reset();
    }

    @AfterAll
    void shutdown() {
        Probe.EXECUTOR.shutdownNow();
    }

    @Test
    @Timeout(25)
    void realClientDisconnectShowsNaiveCouplingAndDecoupledPatterns() throws Exception {
        closeAfterFirstEvent("/naive");
        assertTrue(waitUntil(Probe.NAIVE_RUNTIME_CANCELLED, Duration.ofSeconds(5)));

        closeAfterFirstEvent("/decoupled");
        assertTrue(waitUntil(Probe.DECOUPLED_RESPONSE_CANCELLED, Duration.ofSeconds(5)));
        int ticksAtDisconnect = Probe.DECOUPLED_RUNTIME_TICKS.get();
        Thread.sleep(300);
        assertTrue(Probe.DECOUPLED_RUNTIME_TICKS.get() > ticksAtDisconnect);
        assertFalse(Probe.DECOUPLED_RUNTIME_CANCELLED.get());

        closeAfterFirstEvent("/emitter");
        assertTrue(waitUntil(Probe.EMITTER_RESPONSE_CLOSED, Duration.ofSeconds(5)));
        int emitterTicksAtDisconnect = Probe.EMITTER_RUNTIME_TICKS.get();
        Thread.sleep(300);
        assertTrue(Probe.EMITTER_RUNTIME_TICKS.get() > emitterTicksAtDisconnect);
        assertFalse(Probe.EMITTER_RUNTIME_CANCELLED.get());

        System.out.printf(
                "EVIDENCE E networkClient=true naiveRuntimeCancelled=%s "
                        + "decoupledResponseCancelled=%s decoupledRuntimeContinued=%s "
                        + "emitterResponseClosed=%s emitterRuntimeContinued=%s%n",
                Probe.NAIVE_RUNTIME_CANCELLED.get(),
                Probe.DECOUPLED_RESPONSE_CANCELLED.get(),
                Probe.DECOUPLED_RUNTIME_TICKS.get() > ticksAtDisconnect,
                Probe.EMITTER_RESPONSE_CLOSED.get(),
                Probe.EMITTER_RUNTIME_TICKS.get() > emitterTicksAtDisconnect);
    }

    private void closeAfterFirstEvent(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
        HttpResponse<InputStream> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
        try (InputStream body = response.body()) {
            byte[] firstBytes = body.readNBytes(12);
            assertTrue(firstBytes.length > 0);
        }
    }

    private static boolean waitUntil(AtomicBoolean condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.get() && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        return condition.get();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SseController.class)
    static class TestApplication {}

    @RestController
    static class SseController {
        @GetMapping(value = "/naive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        Flux<ServerSentEvent<String>> naive() {
            return Flux.interval(Duration.ofMillis(40))
                    .map(index -> ServerSentEvent.builder("naive-" + index).build())
                    .doOnCancel(() -> Probe.NAIVE_RUNTIME_CANCELLED.set(true));
        }

        @GetMapping(value = "/decoupled", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        Flux<ServerSentEvent<String>> decoupled() {
            Probe.startDecoupledRuntime();
            return Probe.DECOUPLED_EVENTS.asFlux()
                    .map(data -> ServerSentEvent.builder(data).build())
                    .doOnCancel(() -> Probe.DECOUPLED_RESPONSE_CANCELLED.set(true));
        }

        @GetMapping(value = "/emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        SseEmitter emitter() {
            Probe.startEmitterRuntime();
            SseEmitter emitter = new SseEmitter(0L);
            AtomicBoolean responseOpen = new AtomicBoolean(true);
            emitter.onCompletion(() -> Probe.EMITTER_RESPONSE_CLOSED.set(true));
            emitter.onError(ignored -> Probe.EMITTER_RESPONSE_CLOSED.set(true));
            Probe.EXECUTOR.scheduleAtFixedRate(
                    () -> {
                        if (!responseOpen.get()) {
                            return;
                        }
                        try {
                            emitter.send(SseEmitter.event().data("emitter-event"));
                        } catch (IOException | IllegalStateException exception) {
                            responseOpen.set(false);
                            Probe.EMITTER_RESPONSE_CLOSED.set(true);
                        }
                    },
                    0,
                    40,
                    TimeUnit.MILLISECONDS);
            return emitter;
        }
    }

    static final class Probe {
        static final ScheduledExecutorService EXECUTOR =
                Executors.newScheduledThreadPool(
                        4,
                        runnable -> {
                            Thread thread = new Thread(runnable, "sse-spike");
                            thread.setDaemon(true);
                            return thread;
                        });
        static final AtomicBoolean NAIVE_RUNTIME_CANCELLED = new AtomicBoolean();
        static final AtomicBoolean DECOUPLED_RESPONSE_CANCELLED = new AtomicBoolean();
        static final AtomicBoolean DECOUPLED_RUNTIME_CANCELLED = new AtomicBoolean();
        static final AtomicInteger DECOUPLED_RUNTIME_TICKS = new AtomicInteger();
        static final Sinks.Many<String> DECOUPLED_EVENTS =
                Sinks.many().multicast().directBestEffort();
        static final AtomicBoolean DECOUPLED_STARTED = new AtomicBoolean();
        static final AtomicBoolean EMITTER_RESPONSE_CLOSED = new AtomicBoolean();
        static final AtomicBoolean EMITTER_RUNTIME_CANCELLED = new AtomicBoolean();
        static final AtomicInteger EMITTER_RUNTIME_TICKS = new AtomicInteger();
        static final AtomicBoolean EMITTER_STARTED = new AtomicBoolean();

        static void reset() {
            NAIVE_RUNTIME_CANCELLED.set(false);
            DECOUPLED_RESPONSE_CANCELLED.set(false);
            DECOUPLED_RUNTIME_CANCELLED.set(false);
            EMITTER_RESPONSE_CLOSED.set(false);
            EMITTER_RUNTIME_CANCELLED.set(false);
        }

        static void startDecoupledRuntime() {
            if (DECOUPLED_STARTED.compareAndSet(false, true)) {
                EXECUTOR.scheduleAtFixedRate(
                        () -> {
                            int tick = DECOUPLED_RUNTIME_TICKS.incrementAndGet();
                            DECOUPLED_EVENTS.tryEmitNext("decoupled-" + tick);
                        },
                        0,
                        40,
                        TimeUnit.MILLISECONDS);
            }
        }

        static void startEmitterRuntime() {
            if (EMITTER_STARTED.compareAndSet(false, true)) {
                EXECUTOR.scheduleAtFixedRate(
                        EMITTER_RUNTIME_TICKS::incrementAndGet,
                        0,
                        40,
                        TimeUnit.MILLISECONDS);
            }
        }
    }
}
