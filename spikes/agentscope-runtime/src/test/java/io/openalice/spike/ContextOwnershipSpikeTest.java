package io.openalice.spike;

import static io.openalice.spike.SpikeSupport.assistant;
import static io.openalice.spike.SpikeSupport.occurrences;
import static io.openalice.spike.SpikeSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextOwnershipSpikeTest {

    private static final List<Msg> TURN_TWO_RECONSTRUCTED =
            List.of(user("one"), assistant("reply-1"), user("two"));

    @Test
    void bareAgentSameSessionAddsItsStoredHistoryToOpenAliceReconstruction() {
        SpikeSupport.RecordingModel model = SpikeSupport.RecordingModel.textReplies();
        ReActAgent agent =
                ReActAgent.builder()
                        .name("bare")
                        .model(model)
                        .stateStore(new InMemoryAgentStateStore())
                        .build();
        RuntimeContext context =
                RuntimeContext.builder().userId("bare-shared-user").sessionId("bare-shared").build();

        agent.call(List.of(user("one")), context).block();
        agent.call(TURN_TWO_RECONSTRUCTED, context).block();

        List<Msg> secondModelInput = model.inputs().get(1);
        assertEquals(2, occurrences(secondModelInput, "one"));
        assertEquals(2, occurrences(secondModelInput, "reply-1"));
        assertEquals(1, occurrences(secondModelInput, "two"));
        assertFalse(agent.getAgentState(context).getContext().isEmpty());

        evidence("bare/shared", secondModelInput, agent.getAgentState(context).getContext().size());
    }

    @Test
    void bareAgentFreshRuntimeSessionLeavesOpenAliceInControlOfTurnTwoInput() {
        SpikeSupport.RecordingModel model = SpikeSupport.RecordingModel.textReplies();
        ReActAgent agent =
                ReActAgent.builder()
                        .name("bare-fresh")
                        .model(model)
                        .stateStore(new InMemoryAgentStateStore())
                        .build();

        agent.call(
                        List.of(user("one")),
                        RuntimeContext.builder()
                                .userId("bare-fresh-user")
                                .sessionId("bare-execution-1")
                                .build())
                .block();
        RuntimeContext second =
                RuntimeContext.builder()
                        .userId("bare-fresh-user")
                        .sessionId("bare-execution-2")
                        .build();
        agent.call(TURN_TWO_RECONSTRUCTED, second).block();

        List<Msg> secondModelInput = model.inputs().get(1);
        assertEquals(1, occurrences(secondModelInput, "one"));
        assertEquals(1, occurrences(secondModelInput, "reply-1"));
        assertEquals(1, occurrences(secondModelInput, "two"));
        assertEquals(4, agent.getAgentState(second).getContext().size());

        evidence("bare/fresh", secondModelInput, agent.getAgentState(second).getContext().size());
    }

    @Test
    void selectiveHarnessStillKeepsDelegateContextEvenWithOwnershipHooksDisabled(
            @TempDir Path workspace) {
        SpikeSupport.RecordingModel model = SpikeSupport.RecordingModel.textReplies();
        try (HarnessAgent agent =
                HarnessAgent.builder()
                        .name("selective")
                        .model(model)
                        .stateStore(new InMemoryAgentStateStore())
                        .workspace(workspace)
                        .disableWorkspaceContext()
                        .disableMemoryTools()
                        .disableMemoryHooks()
                        .disableTranscript()
                        .disableSessionPersistence()
                        .disableCompaction()
                        .disableSubagents()
                        .disableFilesystemTools()
                        .disableShellTool()
                        .disableDynamicSkills()
                        .disableDefaultWorkspaceSkills()
                        .skillsEnabled(false)
                        .disableAtPathExpansion()
                        .disableToolsConfig()
                        .disableToolResultEviction()
                        .enableAgentTracingLog(false)
                        .build()) {
            RuntimeContext context =
                    RuntimeContext.builder()
                            .userId("harness-shared-user")
                            .sessionId("harness-shared")
                            .build();
            agent.call(List.of(user("one")), context).block();
            agent.call(TURN_TWO_RECONSTRUCTED, context).block();

            List<Msg> secondModelInput = model.inputs().get(1);
            assertEquals(2, occurrences(secondModelInput, "one"));
            assertEquals(2, occurrences(secondModelInput, "reply-1"));
            assertEquals(1, occurrences(secondModelInput, "two"));
            assertFalse(agent.getDelegate().getAgentState(context).getContext().isEmpty());
            assertTrue(agent.getCompactionHook() == null);

            evidence(
                    "harness/selective-shared",
                    secondModelInput,
                    agent.getDelegate().getAgentState(context).getContext().size());
        }
    }

    @Test
    void selectiveHarnessFreshRuntimeSessionAvoidsHiddenHistoryDuplication(@TempDir Path workspace) {
        SpikeSupport.RecordingModel model = SpikeSupport.RecordingModel.textReplies();
        try (HarnessAgent agent =
                HarnessAgent.builder()
                        .name("selective-fresh")
                        .model(model)
                        .stateStore(new InMemoryAgentStateStore())
                        .workspace(workspace)
                        .disableWorkspaceContext()
                        .disableMemoryTools()
                        .disableMemoryHooks()
                        .disableTranscript()
                        .disableSessionPersistence()
                        .disableCompaction()
                        .disableSubagents()
                        .disableFilesystemTools()
                        .disableShellTool()
                        .disableDynamicSkills()
                        .disableDefaultWorkspaceSkills()
                        .skillsEnabled(false)
                        .disableAtPathExpansion()
                        .disableToolsConfig()
                        .disableToolResultEviction()
                        .enableAgentTracingLog(false)
                        .build()) {
            agent.call(
                            List.of(user("one")),
                            RuntimeContext.builder()
                                    .userId("harness-fresh-user")
                                    .sessionId("harness-execution-1")
                                    .build())
                    .block();
            RuntimeContext second =
                    RuntimeContext.builder()
                            .userId("harness-fresh-user")
                            .sessionId("harness-execution-2")
                            .build();
            agent.call(TURN_TWO_RECONSTRUCTED, second).block();

            List<Msg> secondModelInput = model.inputs().get(1);
            assertEquals(1, occurrences(secondModelInput, "one"));
            assertEquals(1, occurrences(secondModelInput, "reply-1"));
            assertEquals(1, occurrences(secondModelInput, "two"));

            evidence(
                    "harness/selective-fresh",
                    secondModelInput,
                    agent.getDelegate().getAgentState(second).getContext().size());
        }
    }

    private static void evidence(String scenario, List<Msg> modelInput, int stateSize) {
        System.out.printf(
                "EVIDENCE A scenario=%s modelMessages=%d one=%d reply-1=%d two=%d runtimeState=%d%n",
                scenario,
                modelInput.size(),
                occurrences(modelInput, "one"),
                occurrences(modelInput, "reply-1"),
                occurrences(modelInput, "two"),
                stateSize);
    }
}
