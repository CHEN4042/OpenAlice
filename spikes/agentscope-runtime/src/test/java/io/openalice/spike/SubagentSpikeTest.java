package io.openalice.spike;

import static io.openalice.spike.SpikeSupport.toolResponse;
import static io.openalice.spike.SpikeSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.core.tool.subagent.SubAgentTool;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

class SubagentSpikeTest {

    @Test
    @Timeout(15)
    void specialistCompletionReturnsToolResultWithoutBecomingTopLevelAssistantMessage() {
        SpikeSupport.RecordingModel specialistModel =
                new SpikeSupport.RecordingModel(
                        (call, messages) ->
                                Flux.just(SpikeSupport.textResponse("specialist-private-result")));
        SubAgentTool specialistTool =
                new SubAgentTool(
                        () ->
                                ReActAgent.builder()
                                        .name("specialist")
                                        .model(specialistModel)
                                        .build(),
                        SubAgentConfig.builder()
                                .toolName("call_specialist")
                                .forwardEvents(false)
                                .build());
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(specialistTool);
        System.out.println("EVIDENCE D specialistSchema=" + specialistTool.getParameters());
        SpikeSupport.RecordingModel parentModel = parentModel();
        ReActAgent parent =
                ReActAgent.builder()
                        .name("parent")
                        .model(parentModel)
                        .toolkit(toolkit)
                        .maxIters(3)
                        .build();
        RuntimeContext context =
                RuntimeContext.builder().userId("user").sessionId("parent-execution").build();

        Msg candidate = parent.call(List.of(user("delegate privately")), context).block();
        parent.getAgentState(context).getContext().forEach(
                message -> {
                    System.out.println(
                            "EVIDENCE D state role=" + message.getRole()
                                    + " text=" + message.getTextContent()
                                    + " blocks=" + message.getContent().stream().map(block -> block.getClass().getSimpleName()).toList());
                    message.getContentBlocks(ToolResultBlock.class)
                            .forEach(
                                    result ->
                                            System.out.println(
                                                    "EVIDENCE D toolResult state=" + result.getState()
                                                            + " output=" + result.getOutput().stream()
                                                                    .map(block -> block instanceof io.agentscope.core.message.TextBlock text ? text.getText() : block.toString())
                                                                    .toList()));
                });

        assertEquals("parent-candidate-result", candidate.getTextContent());
        assertEquals(1, specialistModel.callCount());
        assertEquals(1, specialistModel.inputs().getFirst().size());
        assertEquals("inspect isolated input", specialistModel.inputs().getFirst().getFirst().getTextContent());

        List<Msg> parentState = parent.getAgentState(context).getContext();
        long specialistToolResults =
                parentState.stream()
                        .flatMap(message -> message.getContentBlocks(ToolResultBlock.class).stream())
                        .count();
        assertEquals(1, specialistToolResults);
        assertFalse(
                parentState.stream()
                        .anyMatch(
                                message ->
                                        "specialist-private-result".equals(message.getTextContent())),
                "specialist answer is a Tool result, not a top-level assistant Message");

        System.out.printf(
                "EVIDENCE D parentSession=%s childModelInputs=%d childInput=%s "
                        + "toolResultsInParent=%d directChildAssistantMessages=%d parentCandidate=%s%n",
                context.getSessionId(),
                specialistModel.inputs().getFirst().size(),
                specialistModel.inputs().getFirst().getFirst().getTextContent(),
                specialistToolResults,
                parentState.stream()
                        .filter(
                                message ->
                                        "specialist-private-result".equals(message.getTextContent()))
                        .count(),
                candidate.getTextContent());
    }

    @Test
    @Timeout(15)
    void disposingParentExecutionCancelsSubagentExecution() throws Exception {
        CountDownLatch childStarted = new CountDownLatch(1);
        AtomicBoolean childCancelled = new AtomicBoolean();
        SpikeSupport.RecordingModel blockedChildModel =
                new SpikeSupport.RecordingModel(
                        (call, messages) ->
                                Flux.<io.agentscope.core.model.ChatResponse>never()
                                        .doOnSubscribe(ignored -> childStarted.countDown())
                                        .doOnCancel(() -> childCancelled.set(true)));
        SubAgentTool specialistTool =
                new SubAgentTool(
                        () ->
                                ReActAgent.builder()
                                        .name("blocked-specialist")
                                        .model(blockedChildModel)
                                        .build(),
                        SubAgentConfig.builder()
                                .toolName("call_specialist")
                                .forwardEvents(false)
                                .build());
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(specialistTool);
        System.out.println("EVIDENCE D blockedSpecialistSchema=" + specialistTool.getParameters());
        ReActAgent parent =
                ReActAgent.builder()
                        .name("parent-cancel")
                        .model(parentModel())
                        .toolkit(toolkit)
                        .maxIters(3)
                        .build();
        RuntimeContext context =
                RuntimeContext.builder().userId("user").sessionId("cancel-parent").build();

        Disposable subscription =
                parent.streamEvents(List.of(user("delegate privately")), context).subscribe();
        assertTrue(childStarted.await(5, TimeUnit.SECONDS));
        subscription.dispose();
        assertTrue(waitUntil(childCancelled, Duration.ofSeconds(3)));

        System.out.printf(
                "EVIDENCE D parentDisposed=true childSubscribed=true childCancelled=%s "
                        + "childModelCalls=%d%n",
                childCancelled.get(), blockedChildModel.callCount());
    }

    private static SpikeSupport.RecordingModel parentModel() {
        return new SpikeSupport.RecordingModel(
                (call, messages) ->
                        call == 1
                                ? Flux.just(
                                        toolResponse(
                                                "call_specialist",
                                                "specialist-call",
                                                Map.of("message", "inspect isolated input")))
                                : Flux.just(SpikeSupport.textResponse("parent-candidate-result")));
    }

    private static boolean waitUntil(AtomicBoolean condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.get() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        return condition.get();
    }
}
