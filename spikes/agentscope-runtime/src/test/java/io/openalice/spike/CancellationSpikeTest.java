package io.openalice.spike;

import static io.openalice.spike.SpikeSupport.toolResponse;
import static io.openalice.spike.SpikeSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class CancellationSpikeTest {

    @Test
    @Timeout(15)
    void targetedAgentInterruptStopsReasoningButDoesNotMeanToolRollback() throws Exception {
        SlowTool tool = new SlowTool(Duration.ofSeconds(2));
        SpikeSupport.RecordingModel model = scriptedSlowToolModel();
        ReActAgent agent = agent(model, tool);
        RuntimeContext context =
                RuntimeContext.builder().userId("user").sessionId("cancel-target").build();
        CountDownLatch terminal = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Instant executionStart = Instant.now();
        agent.streamEvents(List.of(user("run slow tool")), context)
                .doFinally(signal -> terminal.countDown())
                .subscribe(event -> tool.events.add(event.getClass().getSimpleName()), error::set);

        assertTrue(tool.started.await(5, TimeUnit.SECONDS));
        Instant cancelRequest = Instant.now();
        agent.interrupt(context);
        assertTrue(terminal.await(5, TimeUnit.SECONDS));

        Thread.sleep(250);
        assertEquals(1, model.callCount(), "agent must not continue to a second reasoning step");
        assertTrue(tool.completed.get(), "the in-flight Tool continues through targeted interrupt");
        assertFalse(tool.cancelled.get(), "targeted interrupt does not cancel the Tool subscription");

        System.out.printf(
                "EVIDENCE B mechanism=agent.interrupt executionStart=%s toolStart=%s cancelRequest=%s "
                        + "toolCompletedAt=%s streamTerminalAt=%s toolCancelled=%s toolCompleted=%s "
                        + "modelCalls=%d terminalError=%s runtimeMessages=%d events=%s%n",
                executionStart,
                tool.startedAt.get(),
                cancelRequest,
                tool.completedAt.get(),
                tool.streamTerminalAt.get(),
                tool.cancelled.get(),
                tool.completed.get(),
                model.callCount(),
                error.get() == null ? "none" : error.get().getClass().getSimpleName(),
                agent.getAgentState(context).getContext().size(),
                tool.events);
    }

    @Test
    @Timeout(15)
    void disposingEventSubscriptionCancelsReactiveToolButIsNotAProductOutcome() throws Exception {
        SlowTool tool = new SlowTool(Duration.ofSeconds(2));
        SpikeSupport.RecordingModel model = scriptedSlowToolModel();
        ReActAgent agent = agent(model, tool);
        RuntimeContext context =
                RuntimeContext.builder().userId("user").sessionId("disposed-stream").build();

        Disposable subscription =
                agent.streamEvents(List.of(user("run slow tool")), context).subscribe();
        assertTrue(tool.started.await(5, TimeUnit.SECONDS));
        Instant disposedAt = Instant.now();
        subscription.dispose();

        assertTrue(waitUntil(tool.cancelled, Duration.ofSeconds(3)));
        assertFalse(tool.completed.get());
        assertEquals(1, model.callCount());
        assertTrue(
                agent.getAgentState(context).getContext().stream()
                        .anyMatch(message -> "run slow tool".equals(message.getTextContent())),
                "runtime state can retain input even though disposal has no product terminal fact");

        System.out.printf(
                "EVIDENCE B mechanism=subscription.dispose disposedAt=%s toolCancelled=%s "
                        + "toolCompleted=%s modelCalls=%d runtimeMessages=%d%n",
                disposedAt,
                tool.cancelled.get(),
                tool.completed.get(),
                model.callCount(),
                agent.getAgentState(context).getContext().size());
    }

    @Test
    void terminalGateRejectsCandidateThatArrivesAfterCancellation() {
        TerminalGate gate = new TerminalGate();
        assertTrue(gate.cancel());
        assertFalse(gate.acceptCandidate("late assistant completion"));
        assertEquals(TerminalGate.State.CANCELLED, gate.state.get());
        assertTrue(gate.committedAssistant.get() == null);
        System.out.println(
                "EVIDENCE B late-result-gate state=CANCELLED candidateAccepted=false assistantCommitted=false");
    }

    private static ReActAgent agent(SpikeSupport.RecordingModel model, SlowTool tool) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tool);
        return ReActAgent.builder()
                .name("cancellation-spike")
                .model(model)
                .toolkit(toolkit)
                .maxIters(3)
                .build();
    }

    private static SpikeSupport.RecordingModel scriptedSlowToolModel() {
        return new SpikeSupport.RecordingModel(
                (call, messages) ->
                        call == 1
                                ? Flux.just(
                                        toolResponse(
                                                "slow_tool",
                                                "slow-call",
                                                Map.of()))
                                : Flux.just(SpikeSupport.textResponse("normal completion")));
    }

    private static boolean waitUntil(AtomicBoolean condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.get() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        return condition.get();
    }

    static final class SlowTool {
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicReference<Instant> startedAt = new AtomicReference<>();
        final AtomicReference<Instant> completedAt = new AtomicReference<>();
        final AtomicReference<Instant> streamTerminalAt = new AtomicReference<>();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        final List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final Duration delay;

        SlowTool(Duration delay) {
            this.delay = delay;
        }

        @Tool(name = "slow_tool", description = "A deterministic slow cancellable tool")
        public Mono<String> run() {
            return Mono.defer(
                            () -> {
                                startedAt.set(Instant.now());
                                started.countDown();
                                return Mono.delay(delay).map(ignored -> "slow result");
                            })
                    .doOnCancel(() -> cancelled.set(true))
                    .doOnSuccess(
                            ignored -> {
                                completedAt.set(Instant.now());
                                completed.set(true);
                            })
                    .doFinally(ignored -> streamTerminalAt.set(Instant.now()));
        }
    }

    static final class TerminalGate {
        enum State {
            RUNNING,
            COMPLETED,
            CANCELLED
        }

        final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);
        final AtomicReference<String> committedAssistant = new AtomicReference<>();

        boolean cancel() {
            return state.compareAndSet(State.RUNNING, State.CANCELLED);
        }

        boolean acceptCandidate(String text) {
            if (!state.compareAndSet(State.RUNNING, State.COMPLETED)) {
                return false;
            }
            committedAssistant.set(text);
            return true;
        }
    }
}
