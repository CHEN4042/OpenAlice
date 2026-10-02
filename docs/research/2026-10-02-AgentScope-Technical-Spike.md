# OpenAlice AgentScope Technical Spike

> **状态：EXPERIMENT EVIDENCE / NON-AUTHORITATIVE**<br>
> **日期：2026-10-02**<br>
> **基线：** `f1096f81888468297e4062a123d69a81850847dd`<br>
> **分支：** `openalice-20261009`<br>
> **固定版本：** Java 21、AgentScope Java `2.0.3`<br>
> **目的：** 验证哪些 AgentScope runtime capability 可以复用，同时保持 OpenAlice 对 Conversation、Context、Memory 与 Execution 的所有权。本文继续作为非权威实验记录；经 review 接受的稳定结论已提升至 [Architecture Spine](../architecture/ARCHITECTURE-SPINE.md)。

## 1. Executive result

五组实验均取得实际 runtime 证据，没有 BLOCKED 项。推荐：

```text
Outcome B — Bare ReActAgent recommended
```

P1 可以复用 AgentScope core 的 `ReActAgent`、typed event stream、targeted interrupt 与
`SubAgentTool`。OpenAlice 应为每个 Execution 提供自己重建的 Context View，并使用隔离的
runtime session identity；不应让 AgentScope session 延续成为 Conversation history。

Selective Harness 能关闭部分所有权冲突能力，但没有消除 delegate 的 session context。
`disableSessionPersistence()` 在 `2.0.3` 是 no-op；即使关闭 workspace context、memory、
transcript、compaction、subagent、filesystem、shell、skills 等能力，Harness 仍注册
`wait_async_results`、`web_fetch` 和 `web_search`。P1 尚无需要 Harness 的证据。

## 2. Reproduction

完成时的可执行 Spike 位于 `spikes/agentscope-runtime/`，不包含生产应用、根 Maven 工程、
正式 Schema 或外部 LLM。Cycle closing 后临时工程已从 current tree 删除；完整源码保存在 Git
commit [`0d5fd47`](https://github.com/CHEN4042/OpenAlice/tree/0d5fd471c87d14ff9dea1eb0f38b4ef588ad6cd6/spikes/agentscope-runtime)。以下命令针对该 commit 的 checkout。

```bash
cd spikes/agentscope-runtime
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

最终结果：`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。

Raw evidence 在执行时生成于 `spikes/agentscope-runtime/target/surefire-reports/`；`target/` 是
ignored build output，从未提交。可重复证据的长期位置是 commit `0d5fd47` 中的测试源码与本文
观察记录。

## 3. Experiment A — Context and history ownership

### Goal

验证 OpenAlice 显式重建 Turn 2 history 时，Bare `ReActAgent` 与 Selective `HarnessAgent`
是否会额外添加 session history。

### Setup

- Recording Model 保存每次 model-visible `List<Msg>`。
- 每个被测 Agent 显式使用独立的 in-memory runtime StateStore，避免不同场景共享默认存储。
- Turn 1 输入 `one`，模型返回 `reply-1`。
- Turn 2 由 OpenAlice 显式输入 `one / reply-1 / two`。
- 分别测试复用同一 runtime session 和每个 Execution 使用新 runtime session。
- Selective Harness 关闭 workspace context、memory tools/hooks、transcript、compaction、
  subagents、filesystem、shell、skills、`@path`、tools config、tool-result eviction 与 tracing。

### Command / procedure

```bash
mvn -Dtest=ContextOwnershipSpikeTest test
```

### Observed behavior

| Scenario | Turn 2 model messages | `one` | `reply-1` | `two` | Runtime state after call |
| :-- | --: | --: | --: | --: | --: |
| Bare, same session | 5 | 2 | 2 | 1 | 6 |
| Bare, fresh Execution session | 3 | 1 | 1 | 1 | 4 |
| Selective Harness, same session | 5 | 2 | 2 | 1 | 6 |
| Selective Harness, fresh Execution session | 3 | 1 | 1 | 1 | 4 |

Both agents append call inputs and outputs to `AgentState.context`. Reusing that same session while
also sending reconstructed OpenAlice history duplicates old User and Assistant messages. A fresh
runtime session exposes exactly the three messages supplied by OpenAlice.

Harness controls were individually callable, but `disableSessionPersistence()` did not disable
delegate context; official `2.0.3` source labels it “No-op since 2.0; session persistence is owned by
ReActAgent itself.” Compaction was absent after `disableCompaction()`. Workspace, memory and transcript
hooks could be disabled. Some built-in tools remained registered despite the selective configuration.

### PASS / FAIL / BLOCKED

**PASS with required boundary.** OpenAlice can own the input context by using an Execution-scoped
runtime session or an equivalent explicit state reset/store policy. Reusing a Conversation-shaped
AgentScope session while also reconstructing history is incompatible because it duplicates context.

### Architecture implication

- `Conversation ≠ AgentScope session` is confirmed.
- OpenAlice must never combine a long-lived AgentScope transcript with its own complete reconstructed
  Context View.
- AgentScope context is useful only as isolated runtime state inside an Execution boundary.

### Raw evidence location

- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/ContextOwnershipSpikeTest.java`
- Generated report: `target/surefire-reports/io.openalice.spike.ContextOwnershipSpikeTest.txt`

### Comparison

| Behavior | Bare ReActAgent | Selective Harness |
| :-- | :-- | :-- |
| OpenAlice controls input history | Yes, with fresh runtime session | Yes, with fresh runtime session |
| Hidden history duplication | Yes when same session is reused | Yes when same session is reused |
| Session persistence optional | StateStore/session policy must be controlled externally | `disableSessionPersistence()` is a no-op |
| Memory hooks optional | Not bundled | Yes, can disable |
| Compaction optional | Not bundled | Yes, can disable |
| Runtime state isolated from product truth | Yes with Execution-scoped identity | Possible, but more defaults remain |

## 4. Experiment B — Slow Tool cancellation and late result

### Goal

Measure targeted AgentScope interrupt, reactive subscription disposal and the OpenAlice terminal gate.

### Setup

- Recording Model first emits a `slow_tool` call and would emit a normal answer on a second model call.
- Reactive Tool signals subscription start, waits two seconds, then returns.
- One run calls `agent.interrupt(RuntimeContext)` after Tool start.
- A separate run disposes the outer event subscription after Tool start.
- A spike-only terminal gate transitions `RUNNING → CANCELLED` before a late candidate arrives.

### Command / procedure

```bash
mvn -Dtest=CancellationSpikeTest test
```

### Observed behavior

Targeted interrupt sample:

```text
Tool start      02:42:18.262442Z
cancel request  02:42:18.263275Z
Tool completed  02:42:20.267457Z
stream terminal 02:42:20.273344Z
toolCancelled=false
modelCalls=1
```

`agent.interrupt(context)` was cooperative: it did not cancel the in-flight reactive Tool. The Tool
completed about two seconds after the interrupt request, then AgentScope emitted Tool result and
interrupted terminal events without making the second model call. AgentScope retained its runtime
context, including the call input and reconciled Tool/interrupt material; that state remained runtime
evidence rather than an OpenAlice terminal fact.

Disposing the Agent event subscription did cancel the reactive Tool subscription immediately:

```text
toolCancelled=true
toolCompleted=false
modelCalls=1
```

The spike-only terminal gate rejected a candidate after durable state had become `CANCELLED`; no
Assistant Message was committed.

### PASS / FAIL / BLOCKED

**PASS with OpenAlice terminal gate.** AgentScope interrupt prevents later reasoning but does not
guarantee in-flight Tool termination. Subscription disposal and targeted interrupt are observably
different. OpenAlice can still enforce its lifecycle by persisting `CANCELLED` and rejecting late
candidate results.

### Architecture implication

- Cancellation remains an OpenAlice product fact and cannot be inferred from AgentScope stream state.
- Tool cancellation is best-effort; external side effects require their own contract.
- A terminal transition check is mandatory before Timeline commit.

### Raw evidence location

- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/CancellationSpikeTest.java`
- Generated report: `target/surefire-reports/io.openalice.spike.CancellationSpikeTest.txt`

## 5. Experiment C — Hard process crash and restart

### Goal

Confirm that a real JVM hard kill can be explained and reconciled only from OpenAlice durable facts.

### Setup

- Parent JUnit process creates a temporary directory.
- Process A stores one experimental row in temporary SQLite: User Message, `RUNNING` Execution and
  null Assistant Message.
- Process A starts a real `ReActAgent` call backed by a never-completing deterministic Model and writes
  a marker only after the runtime subscription is active.
- Parent calls `destroyForcibly()` and waits for process exit.
- Restart probe opens the same SQLite file and updates stale `RUNNING → INTERRUPTED`.

### Command / procedure

```bash
mvn -Dtest=CrashRestartSpikeTest test
```

### Observed behavior

```text
hard-kill exit=137
before restart: status=RUNNING, User Message present, Assistant Message absent
after reconciliation: status=INTERRUPTED, User Message present, Assistant Message absent
AgentScope state required=false
```

This was a separate JVM hard kill, not an exception or graceful shutdown. No graceful shutdown state
was used as crash evidence.

### PASS / FAIL / BLOCKED

**PASS.** OpenAlice durable facts alone identified and reconciled the crashed Execution.

### Architecture implication

The current crash/restart invariant and `INTERRUPTED` state are executable without treating
AgentScope session/state as product truth.

### Raw evidence location

- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/CrashRestartSpikeTest.java`
- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/CrashChildMain.java`
- Generated report: `target/surefire-reports/io.openalice.spike.CrashRestartSpikeTest.txt`

## 6. Experiment D — Specialist/subagent completion

### Goal

Test whether a child Agent can return an internal result without directly creating a main
Conversation Message, and whether parent subscription cancellation reaches the child.

### Setup

- Bare parent `ReActAgent` invokes AgentScope core `SubAgentTool` named `call_specialist`.
- Child receives only `inspect isolated input` and returns `specialist-private-result`.
- Parent model sees the Tool result and returns `parent-candidate-result`.
- A second scenario uses a never-completing child and disposes the parent execution subscription.

### Command / procedure

```bash
mvn -Dtest=SubagentSpikeTest test
```

### Observed behavior

- Child model received one isolated User Message.
- Parent runtime state contained one successful `ToolResultBlock` with child result and generated
  child `session_id`.
- Parent state contained zero top-level Assistant Messages equal to the child result.
- Parent returned its own `parent-candidate-result` for the Conversation Application to accept or reject.
- Disposing the parent subscription caused `SubAgentTool` to interrupt the child and cancel its model
  subscription (`childCancelled=true`).
- No AgentScope persistence automatically wrote a product Conversation.

### PASS / FAIL / BLOCKED

**PASS for Bare ReActAgent + core SubAgentTool.** The child result remains a Tool/candidate result.
Selective Harness subagent orchestration was intentionally disabled and was not needed to prove the
boundary.

### Architecture implication

The desired chain is supported:

```text
Specialist completion
→ Tool/candidate Result
→ parent candidate outcome
→ Conversation Application decision
→ optional Timeline Message
```

### Raw evidence location

- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/SubagentSpikeTest.java`
- Generated report: `target/surefire-reports/io.openalice.spike.SubagentSpikeTest.txt`

## 7. Experiment E — Spring SSE disconnect adapter

### Goal

Use real network connections to verify that HTTP response lifecycle can be separated from runtime
Execution lifecycle.

### Setup

An embedded Spring MVC server on a random port exposes three test-only endpoints:

1. Naive MVC `Flux<ServerSentEvent<?>>` directly backed by a runtime publisher.
2. Decoupled MVC `Flux<ServerSentEvent<?>>` backed by an independently running producer and event sink.
3. `SseEmitter` fed by an independently running producer.

Java `HttpClient` connects to each endpoint, reads the first event bytes and closes the actual TCP/HTTP
response body early.

### Command / procedure

```bash
mvn -Dtest=SseDisconnectSpikeTest test
```

### Observed behavior

| Adapter | Response observed disconnect | Runtime after disconnect |
| :-- | :-- | :-- |
| Naive directly bound MVC Flux | Yes | Publisher cancelled |
| Decoupled MVC Flux | Yes | Independent runtime kept ticking |
| Decoupled SseEmitter | Yes through completion/error/send failure | Independent runtime kept ticking |

Actual evidence line:

```text
networkClient=true naiveRuntimeCancelled=true decoupledResponseCancelled=true
decoupledRuntimeContinued=true emitterResponseClosed=true emitterRuntimeContinued=true
```

### PASS / FAIL / BLOCKED

**PASS.** Both MVC reactive return and `SseEmitter` can preserve the architecture invariant if the
adapter does not bind runtime ownership to the response subscription. The naive direct binding does
couple browser disconnect to runtime cancellation.

### Architecture implication

- `SSE disconnect ≠ Execution cancellation` is confirmed.
- Response cleanup callback should detach delivery resources only.
- Explicit cancel by Execution identity remains the only product cancellation command.
- Architecture need not select `SseEmitter` versus MVC reactive return.

### Raw evidence location

- Git `0d5fd47`：`spikes/agentscope-runtime/src/test/java/io/openalice/spike/SseDisconnectSpikeTest.java`
- Generated report: `target/surefire-reports/io.openalice.spike.SseDisconnectSpikeTest.txt`

## 8. Final decision matrix

| Capability | Bare ReActAgent | Selective Harness | Recommended boundary |
| :-- | :-- | :-- | :-- |
| OpenAlice-owned context | PASS with Execution-scoped runtime session | PASS with same restriction | OpenAlice supplies complete Context View per Execution |
| Runtime state | Explicit `AgentState` remains | Delegate `AgentState` plus Harness defaults | Runtime-only; never product truth |
| Memory/session hooks | No Harness memory hooks | Hooks can be disabled; session-disable is no-op | Do not use Harness memory/session ownership |
| Cancellation | Cooperative targeted interrupt | Delegates to same core behavior | Persist OpenAlice terminal fact first; request runtime stop second |
| Late result handling | OpenAlice gate rejects candidate | Same gate still required | Terminal-state compare before Timeline commit |
| Crash/restart | OpenAlice SQLite facts sufficient | Harness state unnecessary | Reconcile stale `RUNNING → INTERRUPTED` |
| Subagent isolation | Core `SubAgentTool` returns Tool result cleanly | Harness orchestration not required for P1 | Specialist result remains candidate data |
| SSE integration | Typed event stream can feed decoupled adapter | No Harness benefit observed | Response subscription never owns Execution |

## 9. Recommendation

### Selected runtime boundary

Use Bare `ReActAgent` for P1 behind an OpenAlice `Agent Runtime` adapter.

Reuse:

- deterministic model/tool invocation interface;
- typed `AgentEvent` stream;
- targeted `interrupt(RuntimeContext)` as a best-effort runtime signal;
- core `SubAgentTool` when specialist execution is later in accepted scope.

OpenAlice retains:

- Context reconstruction and admission;
- per-Execution runtime identity;
- durable Execution state and cancellation fact;
- late-result acceptance gate;
- Timeline commit;
- crash/restart reconciliation.

### Harness controls tested

Harness is not selected. If later evidence reopens it, the tested minimum disable set is:

```text
disableWorkspaceContext
disableMemoryTools
disableMemoryHooks
disableTranscript
disableCompaction
disableSubagents
disableFilesystemTools
disableShellTool
disableDynamicSkills
disableDefaultWorkspaceSkills
skillsEnabled(false)
disableAtPathExpansion
disableToolsConfig
disableToolResultEviction
enableAgentTracingLog(false)
```

Do not rely on `disableSessionPersistence()`; it is a no-op in `2.0.3`. This configuration still left
some built-in tools registered, so adopting Harness would require another explicit capability audit.

## 10. Architecture Impact

### Architecture confirmed

- OpenAlice can own Conversation and Context while AgentScope supplies replaceable execution.
- Conversation and AgentScope session must remain distinct.
- Cancellation needs OpenAlice durable terminal state and late-result rejection.
- Hard restart can reconcile `RUNNING → INTERRUPTED` without AgentScope state.
- Specialist completion can remain candidate Result data.
- A decoupled Spring adapter preserves `SSE disconnect ≠ Execution cancellation`.

### Architecture amendment candidate

None required. Current Spine already describes cancellation as a request to Agent Runtime and requires
terminal transition protection. User + Web may choose to clarify, without changing the invariant, that
AgentScope `2.0.3` targeted interrupt does not cancel an already running reactive Tool.

### Implementation choice

- Configure an explicit runtime-only AgentStateStore and allocate a runtime session identity per
  OpenAlice Execution, or otherwise start from explicitly empty AgentScope state before supplying an
  OpenAlice Context View.
- Use Bare `ReActAgent` in the first integration.
- Persist `CANCELLED` before accepting runtime completion and compare terminal state during outcome commit.
- Decouple runtime subscription from the MVC Flux or `SseEmitter` response lifecycle.
- Treat disconnect callbacks as delivery cleanup signals.

### Still unknown

- Cancellation and external side effects for blocking/non-reactive production Tools.
- Exact timeout and compensation contract for future Action Tools.
- Graceful shutdown persistence behavior; it was deliberately not treated as hard-crash evidence.
- Harness `AgentSpawnTool` behavior under a fully enabled Harness; P1 does not require it.
- Final choice between decoupled MVC Flux and `SseEmitter`; both preserved the invariant in this spike.
- Production AgentStateStore retention/cleanup, because P1 should not use it as durable Conversation.

## 11. Fixed official references

- [AgentScope Java v2.0.3 release](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.3), commit `1b8e3dc`.
- [`HarnessAgent` v2.0.3](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/agentscope-harness/src/main/java/io/agentscope/harness/agent/HarnessAgent.java).
- [`ReActAgent` v2.0.3](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/agentscope-core/src/main/java/io/agentscope/core/ReActAgent.java).
- [`SubAgentTool` v2.0.3](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/agentscope-core/src/main/java/io/agentscope/core/tool/subagent/SubAgentTool.java).
- [Spring MVC asynchronous requests and SSE](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html).
