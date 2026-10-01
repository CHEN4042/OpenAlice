# Background / Task / Proactivity 第二轮调研

> **状态：WORKING NOTE / NON-AUTHORITATIVE**<br>
> **日期：2026-10-01**<br>
> **Run label：GPT-5.6 Sol architecture research**<br>
> **范围：** Reminder、Scheduled Task、long-running agent task、background completion、memory consolidation、proactive interaction；不建立模块树，不决定数据库，不授权实现。<br>
> **对照基线：** [第一轮 Agent 架构关键问题调研](2026-10-01-Agent架构关键问题调研.md)。

## 方法与证据边界

检索词围绕 `background task durability`、`scheduled task run identity`、`completion delivery idempotency`、`proactive assistant interruption`、`AgentScope Java cancellation state persistence scheduler` 展开。优先阅读官方文档、固定 revision 的源码与测试，再用论文检查 proactivity 的交互和隐私边界。

本轮是静态文档、源码和测试阅读，没有运行外部项目或构建 prototype。源码测试证明被检查 revision 的预期行为，不证明 OpenAlice 集成后的端到端保证；所有设计结论均标为 Interpretation / OpenAlice Implication 或 UNKNOWN / NEEDS PROTOTYPE。

## 结论摘要

1. **Background 不是一个生命周期。** Reminder / Scheduled Task / condition trigger 主要描述“为何开始”；Agent Run、Memory Consolidation 与 Delivery 各有不同恢复和副作用边界；Proactivity 决定“是否及何时打扰用户”。把它们压成一个通用 Task Engine 会隐藏差异。
2. **Definition、Occurrence / Run、Execution、Result、Delivery 必须能区分身份。** Recurring definition 与每次 occurrence 不能共用一个 identity；否则无法表达某次取消、重试、完成但未送达或去重。
3. **持久化结果先于对话呈现。** 默认候选是 background result 先成为可恢复的 Task Result，再由 Conversation Application / Character policy 决定是否、何时以及以谁的身份进入长期 Conversation。Execution completion 不等于 proactive message。
4. **P1 不需要 scheduler 或 workflow platform。** 现在应保留 identity、provenance、状态边界和可替换 timing mechanism；真正加入用户可见 reminder 时再实现 durable definition/run/delivery。Spring `TaskScheduler` 适合 timing，不能替 OpenAlice 定义业务语义；Quartz、Temporal 只在需求跨过相应门槛后考虑。
5. **AgentScope 是 execution capability。** 2.0.3 已有 per-session 串行、run handle、事件流、取消/中断和 AgentState；但 runtime interrupt 不持久、state 多在 call 结束保存，scheduler extension 每次 trigger 创建 fresh Agent。这些不能代替 OpenAlice Task / Conversation / Delivery semantics。

## 1. Background work taxonomy

| 概念 | 触发或目的 | 独有 lifecycle / 状态 | 与其他概念的关系 |
| :-- | :-- | :-- | :-- |
| Reminder | 用户要求在某时提醒 | definition、due occurrence、ack / dismiss、delivery outcome | 可以使用 one-shot scheduled trigger；价值主要在 delivery，而非 agent reasoning |
| Scheduled Task | 指定时间、间隔或 cron 重复执行工作 | stable definition、每次 occurrence、enable / pause、misfire / catch-up | 一项 definition 产生多个独立 run |
| Condition-triggered Task | 观察外部状态，条件成立才执行 | observation cursor/state、dedupe、condition evaluation、fired run | 时间只可能是 polling trigger；条件状态不能只放模型上下文 |
| Long-running / Async Agent Task | 需要脱离 foreground turn 的执行 | admission、run、attempt、progress、cancel、timeout、terminal result | 可由任何 trigger 创建；不必有 schedule |
| Agent Completion Delivery | 已完成结果的呈现或通知 | result ready、delivery eligibility、attempt、delivered / failed / unknown | execution 的下游；不是 execution terminal state 的同义词 |
| Memory Consolidation | 从已存在资料派生 summary / memory | input range/version、algorithm version、output lineage、supersede / rollback | 通常是内部维护 run，不应默认产生用户消息 |
| Proactive Conversation / Outreach | 系统决定主动联系用户 | eligibility、policy evaluation、suppression、channel selection、user feedback | 可消费 reminder/result/event，但 completion 不自动授权 outreach |

**Observed Fact：** OpenClaw 当前文档把 Automations、Heartbeat、Hooks 和 standing instructions 分开；Automations 持久化 schedule，Heartbeat 是有 busy guard 和 silent outcome 的 ambient monitor。当前 main 又移除了之前的共享 Tasks ledger / TaskFlow，把 subagent、ACP 与 automation completion 交回各自 runtime。[S1][S2]

**Interpretation：** “background” 是部署/交互位置，不是足够的领域抽象。Trigger 可以共享 timing mechanism，但 Reminder、agent run、memory derivation 和 user interruption 不能共享一套含混状态机。

**OpenAlice Implication：** 保留共同 correlation / execution coordination 语言；按真实生命周期建模。当前不命名一个总揽所有概念的 `TaskEngine`。

## 2. Task definition、trigger 与 run identity

候选身份链：

```text
Task Definition
    └─ Scheduled / Condition Trigger
         └─ Occurrence / Run
              └─ Agent Execution (optional, one or more attempts)
                   └─ Result
                        └─ Delivery / Notification attempt
```

| Identity | 回答的问题 | 是否跨重启 | 是否一对一 |
| :-- | :-- | :-- | :-- |
| `definitionId` | 用户长期定义了什么工作？ | 是 | 一个 recurring definition 对多个 run |
| `triggerId` / trigger version | 什么规则产生 occurrence？ | 是 | definition 可换 schedule / condition |
| `runId` / `occurrenceId` | 这一次应做什么？ | 是 | 每次 occurrence 唯一 |
| `attemptId` | 此 run 第几次实际尝试？ | 是，至少记录历史 | 一个 run 可有多次 attempt |
| `agentExecutionId` | 哪一次 Agent Runtime 调用？ | 至少可关联 | 不一定存在，也可能多次 |
| `resultId` | 哪个不可变或版本化结果待消费？ | 是 | 可被多个 delivery 消费 |
| `deliveryId` / idempotency key | 向哪个 destination 呈现哪份 result？ | 是 | destination / mode 不同则不同 |

**Observed Fact：** Quartz 区分 durable `JobDetail` / `JobKey`、Trigger 与某次触发的 `fireInstanceId`；OpenClaw 保存 job definition、runtime state、run history，并以 job/run idempotency key 防止同一结果重复写入 current session。[S3][S4] Temporal 也区分 Workflow Execution 与可能多次执行的 Activity Task attempt。[S5]

**Interpretation：** UI 中一句“每天九点提醒”至少包含 definition 与 occurrences。把“任务 ID”同时用于定义、运行和送达，会使某一次取消、补跑和重复通知无法精确表达。

**OpenAlice Implication：** 即使初版只支持 one-shot reminder，也不要让 schedule row、agent execution session 和 conversation message 共享一个 ID。

## 3. Durability 与 delivery guarantees

### 3.1 跨进程重启需要保留什么

| 状态 | 最低持久要求 | 恢复原则 |
| :-- | :-- | :-- |
| Task definition / trigger | 定义版本、时区、next occurrence、enabled state | 重算 next fire 必须保留 schedule 语义与版本 |
| Queued / admitted run | `runId`、cause、definition version、requested time、admission state | 重启后可确定“未开始、可重试、已被取消” |
| Running run | attempt、lease/owner、started time、last safe checkpoint | 进程消失后标记 interrupted / unknown；新 attempt 不能冒充原 attempt |
| Completed result | result、provenance、terminal status、completed time | 先持久化再进入 delivery；可在未送达时恢复 |
| Retry | attempt count、next retry、last error class、policy version | 重试沿用 `runId`，产生新 `attemptId` |
| Cancellation | requested time / actor / reason、terminal acknowledgement | runtime signal 可丢，但取消意图不能只在内存中 |
| Delivery | destination、policy decision、attempt、receipt、delivered / failed / unknown | unknown 不自动重发有副作用的发送；先核验或等待用户处理 |

**Observed Fact：** AgentScope 的 `InterruptControl` 属于某次 invocation，不进入 `AgentState`；同 session 的 interrupt 可生成 recovery reply 并保存对话，但 hard cancel 不承诺 state-save 路径或回滚外部副作用。[S6][S7] OpenClaw 会在发送前记录 durable delivery attempt；若崩溃发生在“可能已发出但未确认”的窗口，结果保持 `Unknown`，不会把任意副作用包装成 exactly-once。[S4]

**Interpretation：** “running” 无法仅靠重启后没有线程来判定失败或可安全重跑。需要 lease / receipt / reconciliation；外部 Action 仍需 provider receipt、查询核验或稳定 idempotency key。

### 3.2 At-most-once、at-least-once 与 exactly-once

- **At-most-once：** 不重试可避免重复，但 crash window 会丢任务或通知。
- **At-least-once：** durable queue + retry 更适合内部工作，但 handler / side effect 必须用稳定 operation key 幂等，或者把无法判断的结果标为 `UNKNOWN`。
- **Exactly-once：** 不能对跨数据库、模型、第三方 channel / tool 的完整链路轻易承诺。Temporal 对 Workflow code 的 durable execution 承诺不意味着每个外部 Activity 副作用天然 exactly-once；其官方材料也把 Activity 描述为可多次 attempt 的 effectively-once experience。[S5]

**OpenAlice Implication：** 内部状态更新优先使用唯一约束/compare-and-set；Conversation write 使用 `(resultId, presentationKind)` 去重；外部通知使用 `(resultId, destination, policyVersion)` 生成稳定 idempotency key，并保存 receipt / `UNKNOWN`。

## 4. Foreground 与 background coordination

以下是 architecture responsibility，不是最终产品 policy：

| 同时事件 | 默认候选动作 | 谁决定 / 谁执行 |
| :-- | :-- | :-- |
| 用户发新消息，Alice 正在回答 | 同 Conversation 排队；显式 stop 才 cancel / interrupt | Conversation Application 决定；Execution Coordination 定序；Agent Runtime 执行取消 |
| Kei 想参与，Alice turn 仍在进行 | delay / merge 为后续 character turn；不得并发直接写 Timeline | Character participation policy + Conversation Application |
| Reminder 到期，foreground busy | 保存 due occurrence；按 urgency / policy delay 或 deliver later | Task semantics 记录 due；Proactivity / Delivery policy 决定时机 |
| Specialist / background research 完成 | 持久化 Result；不直接抢占 foreground；可排 user-visible presentation | Background owner + Conversation Application |
| Memory consolidation 运行中，用户新消息到达 | 用户 turn 优先；consolidation 使用固定 input version，冲突则重算或放弃提交 | Memory lifecycle owner；不得锁住 Conversation |
| 用户取消 background run | 持久化 cancellation intent；通知 runtime；晚到 result 依据 terminal transition 拒收或标记 | Task semantics + Runtime adapter |

**Observed Fact：** AgentScope 2.0.3 对相同 `(userId, sessionId)` 的 call 串行，不同 session 并行；`AgentRun` 可区分 queued / running / terminal，queued cancellation 不会越过前一个 run。[S6][S8] OpenClaw Heartbeat 在 main queue、automation 或目标 session busy 时 defer / skip；background completion 可触发 wake，但仍受 conversation busy guard。[S2]

**Interpretation：** Session lane 能解决 execution serialization，却不知道 Reminder 是否紧急、Kei 是否应发言、Result 是否值得打扰用户。定序机制与产品 policy 必须分层。

**OpenAlice Implication：** 单进程先用 per-conversation coordination + 持久 run transition；无需 Gateway/Worker 拆分。对 Timeline 的 user-visible write 只有 Conversation Application 能提交。

## 5. Proactivity 是独立 policy decision

候选决策链：

```text
Result / Due Occurrence / Observed Event
    → eligible for presentation?
    → allowed to interrupt now?
    → choose character, wording and channel
    → durable delivery attempt
```

至少需要独立考虑 quiet hours、cooldown、rate limit、relevance / urgency threshold、recent user activity、pending foreground conversation、duplicate suppression、user control、notification channel、privacy / context visibility 与角色知情范围。

**Observed Fact：** OpenClaw 的 proactive Heartbeat 支持 active hours、busy deferral、silent `NO_REPLY`、target suppression，并明确 completion event 只会唤醒一次 policy-bearing agent turn。[S2] 人类打断研究显示人会根据 urgency 和被打断任务线索调整时机；SOUPS 2022 的研究参与者重视对 assistant action / data 的控制，并普遍重视少打扰。[S9][S10]

**Interpretation：** “任务完成”是事实；“Alice 现在应主动联系用户”是可撤销、用户可控且依赖上下文的策略判断。二者合并会让每次后台完成自动变成骚扰或隐私泄漏。

**OpenAlice Implication：** Result 默认可被查询但不可见；只有 policy evaluation 产出的 Delivery Intent 才尝试通知。用户显式 Reminder 可以有较高 delivery priority，但仍需 quiet-hours / channel 规则的明确产品决定。

## 6. Background result 如何进入长期 Conversation

| 模型 | 优点 | 主要问题 | 结论 |
| :-- | :-- | :-- | :-- |
| A. Execution 直接写 Conversation | 简单、低延迟 | runtime 冒充产品角色；chronology race；重复写；crash 后难判断是否已呈现 | **Do not use as default** |
| B. 先保存 Task Result，再由 Application 呈现 | result 与 delivery 可独立恢复；易去重；保留 provenance | 多一步状态转换；需明确 presentation policy | **Preserve boundary now** |
| C. Internal event → Character/Application 生成发言 | 能按当前 persona / context 形成自然角色发言 | 二次模型调用可能改变事实；事件重放需去重；生成失败不应丢 result | **Use selectively above B** |

候选组合是 **B 作为 durable base，C 作为可选 presentation**。确定性的 Reminder 文本也先有 occurrence/result，再由 Conversation Application 用明确角色和 provenance 写 Timeline。Memory consolidation output 不写 Conversation；需要告知用户时生成独立 presentation。

**Observed Fact：** OpenClaw current-session delivery 等待 active turn，检查 session generation，通过 canonical transcript writer 提交带 job/run provenance 与 idempotency key 的结果；重试不能重复 append。[S4]

**OpenAlice Implication：** Conversation chronology 由 Conversation Application 统一决定；Result 保留 `producedByExecutionId`、source inputs、character / specialist attribution 和 presentation status。Conversation Message 记录 `presentedFromResultId`，不复制伪造 User Message。

## 7. Scheduler boundary

原则候选：

> **OpenAlice owns task semantics; a library/framework owns timing and wakeup mechanics.**

| 机制 | 真正解决的问题 | 不替 OpenAlice 解决什么 | 当前判断 |
| :-- | :-- | :-- | :-- |
| Spring `TaskScheduler` / `@Scheduled` | 单进程 timer、cron/fixed rate、线程调度 | durable user definition、run/result/delivery、restart reconciliation；重复 annotation 还可并行重叠 | 内部低风险 maintenance 可用；不能单独承载 durable Reminder |
| Quartz JDBCJobStore | 持久 Job/Trigger、misfire、clustering、recovery hooks、concurrency annotation | 产品 Task/Result/Delivery、外部副作用幂等、proactivity | 有 durable scheduling 需求时再 prototype；P1 不引入 |
| AgentScope scheduler extension | Quartz / XXL-Job timing adapter，每次 trigger 创建 fresh Agent | OpenAlice definition、conversation integration、delivery | 可作为未来 adapter 候选，不建立所有权 |
| LangGraph durable execution | checkpoint / replay、task result 恢复、interrupt | OpenAlice 长期 Conversation、Character 与主动通知 policy | 复杂 graph 出现后再比较；P1 无此需求 |
| Temporal | 长期 durable workflow、history/replay、timer、retry、worker failover | 产品语义与外部副作用天然 exactly-once | 多步骤、跨日、补偿/人工暂停成为核心需求后再评估；当前过重 |

**Observed Fact：** Spring 官方文档的 `TaskScheduler` 实现基于本地 `ScheduledExecutorService` 等 timing mechanism，并警告重复 `@Scheduled` 可重叠；Quartz 2.5.x 的 JDBCJobStore 把 Job/Trigger 存入关系数据库并提供 misfire / cluster 配置。[S11][S12] AgentScope scheduler extension 自己选择 Quartz / XXL-Job，并在每次 trigger 创建 fresh Agent。[S13] LangGraph 的 durable execution 会从 checkpoint replay，并要求把 non-deterministic work / side effects 放入 task 且保持 idempotent。[S14]

**OpenAlice Implication：** 不为“将来可能需要”预建 scheduler abstraction hierarchy。第一次实现 user-visible Reminder 时，用 acceptance test 驱动选择：restart、timezone/DST、misfire、cancel、duplicate due、result/delivery recovery。

## 8. Event architecture：按失败边界选择机制

| 机制 | 适用场景 | 当前需要 |
| :-- | :-- | :-- |
| Direct Java call | 同一 transaction / request 中必须立即得到结果的同步协调 | **是，默认** |
| In-process application/domain event | 同进程多个非关键观察者；丢失后可重建或无害 | **按需**，不用于唯一 durable handoff |
| Persistent task queue / outbox | commit 后必须最终执行、跨重启、可重试且需审计的 handoff | 加入 durable background / delivery 时引入最小实现 |
| External broker | 跨进程 fan-out、独立扩缩、吞吐/隔离已成为现实约束 | **Do not build yet** |

**Interpretation：** Event 是边界后的事实或请求，不是所有内部调用的统一包装。若“写 result”与“安排 delivery”必须共同恢复，应由同一持久化 transaction / outbox 保证，而不是发一个可能丢失的进程内 event。

**OpenAlice Implication：** 当前不建大一统 Event Bus。先定义少量稳定 transition（如 `RunCompleted`、`DeliveryRequested`）及其幂等消费条件，只有真实异步需求出现才落地。

## 9. AgentScope Java 2.x interaction

本轮固定官方 release `v2.0.3`（2026-09-07），源码检查 commit `e9721285c63a37c10b1d07aa540408e57ba56ab2`（2026-10-01）。

| 能力 | Observed Fact | OpenAlice boundary |
| :-- | :-- | :-- |
| Async / streaming | `call` 返回 `Mono<Msg>`；`streamEvents` 输出 typed `AgentEvent`；`AgentRun` 暴露 run id / status | 可映射 execution progress，不等于 durable Task Result |
| Ordering | 同 `(userId, sessionId)` 自动串行；不同 session 并行，测试覆盖 no lost update | 可复用 runtime lane；Conversation Application 仍拥有 turn order |
| Cancellation | run handle 支持 cancel / cooperative interrupt；subagent test 验证 disposal 会 interrupt + cancel | OpenAlice 持久化 cancel intent；tool 外部副作用另行 reconcile |
| State persistence | `AgentState` 在 call entry load、call end save；失败流保存安全的 user input，不保存 incomplete output | 适合 execution recovery candidate，不作为 OpenAlice Conversation source of truth |
| Session persistence | `(userId, sessionId)` 选择 state slot；state 包含 context、summary、permission/tool state | AgentScope session identity 不等于 Conversation / Task identity |
| Subagent / completion | Harness 有 async subagent、spawn registry / wakeup 相关实现与 event forwarding | completion presentation 与角色归属仍由 OpenAlice 决定 |
| Scheduler | extension 提供 Quartz / XXL-Job adapter，并 fresh-create Agent | timing/execution adapter，不拥有 Task Definition / Delivery |

**NEEDS PROTOTYPE：** 验证 bare `ReActAgent` 与 selective `HarnessAgent` 两条路径；至少覆盖 call 中途 crash、queued cancel、blocking tool、subagent late completion、state-store CAS conflict、重复 delivery、重启后的 OpenAlice Conversation 与 AgentState 对账。不要用 AgentScope `sessionId` 直接充当所有 product identity。

## 10. 与第一轮研究的对照

| 主题 | Previous finding | New evidence | 判断与原因 | OpenAlice implication |
| :-- | :-- | :-- | :-- | :-- |
| Execution coordination | 单用户仍需 session ordering / cancellation；不等于 Gateway service | AgentScope 2.0.3 已有 same-session gate 与 per-run handle；OpenClaw busy guard 区分 main / automation / target session | **Agreement + refinement**：runtime lane 可复用，但 policy / durable cancellation 仍在 OpenAlice | 单进程 coordination 足够起步；建立 adapter contract，不拆服务 |
| Foreground/background collision | Reminder / completion 与用户输入需排队、延后或取消规则 | OpenClaw Heartbeat 在 busy 时 defer，completion wake 也不绕过 session guard | **Agreement**：先保存事实，再按 policy 安排 presentation | Conversation Application 是 Timeline 唯一写 owner |
| Completion delivery | 完成状态不能靠 SSE；需要去重和延迟汇报 | OpenClaw 保存 result/delivery attempt，明确 `Unknown` crash window，并用 job/run key 去重 transcript commit | **Refinement**：Result 与 Delivery 应是两段状态机 | 默认采用 durable Result → policy → Delivery Intent |
| Durable state | 后台前需要持久 task state；不承诺 exactly-once | AgentScope interrupt signal 不持久；Temporal / LangGraph 重放仍要求 side effect 幂等；Quartz 只持久 timing | **Agreement + refinement**：明确 definition/run/attempt/result/delivery identities | 用 at-least-once + idempotency / reconciliation；记录 unknown |
| Event boundaries | 内部无需大一统 Event Bus；真正异步才用 event | Outbox 才能覆盖 commit-to-background crash gap；broker 不增加单进程正确性 | **Agreement** | direct call 默认；durable handoff 出现时用最小 outbox/queue |
| AgentScope ownership | Harness 内建 state/context/compaction 可能与产品语义重复，需 prototype | 2.0.3 明确 AgentState 内部拥有 conversation context，且 call 粒度保存；scheduler fresh-creates Agent | **Agreement + stronger boundary**：能力增强没有消除双重所有权风险 | Conversation/Task/Delivery 保持 OpenAlice authority；选择性采用 runtime features |
| OpenClaw 结构 | 第一轮引用 session lane、后台 task ledger 与 completion queue | 当前 `cd8907aa` 已移除共享 Tasks ledger / TaskFlow，改由各 runtime 拥有 execution / completion | **Disagreement with using its old module shape as a model**：项目快速演化证明结构不可照搬；核心语义分离仍成立 | 借鉴 failure cases / invariants，不复制 OpenClaw 当前或旧模块树 |

第二轮没有推翻第一轮的核心边界：轻量协调、持久结果、去重交付、OpenAlice 拥有产品语义。它推翻的是把某个外部项目当作稳定模块模板的可能性，并进一步要求把 completion 与 proactivity 分开。

## 11. Recommendation boundaries

### Adopt now

- 记录 Conversation / Turn / Message / Character / Execution 的独立 identity 与 provenance。
- Conversation Application 独占 user-visible Timeline commit。
- 将“execution finished”“result persisted”“delivery decided”“delivery confirmed”视为不同 transition。

### Preserve boundary now

- P1 若创建相关数据，至少保留 `conversationId`、`turnId`、`messageId`、`characterId`、`personaVersion`、`executionId`、`sourceKind/sourceId` 与 causal link；不要让 AgentScope session 成为唯一 source of truth。
- 第一个 background feature 出现时保留 `definitionId`、`runId`、`attemptId`、`resultId`、`deliveryId`、status/version/timestamps、idempotency key 与 provenance。字段可以先是同进程实现，不代表预建完整模块。
- Memory consolidation 记录 input range/version、processor version 和 output lineage。

### Prototype before deciding

- AgentScope bare ReAct vs selective Harness，包括 crash/cancel/state ownership。
- Spring timer + application-owned durable rows 是否满足首个 Reminder；若 misfire / concurrency / ops 复杂度上升，再比较 Quartz。
- Result → Conversation presentation 的 transaction / outbox 方案和 duplicate simulation。

### Defer to P2

- P1 不实现 user-visible Reminder / scheduled task、background research、memory consolidation cadence、proactivity policy 与管理 UI；这些进入 P2 或更后续的产品优先级讨论，不代表承诺在 P2 全部交付。
- Condition trigger、multi-character proactive participation 与多 channel delivery。

### Do not build yet

- 通用 Task Engine、workflow DSL、external broker、Gateway/Worker 拆分、Temporal cluster、microservices、全局 Event Bus。
- exactly-once 宣称；自动把 completion 写成 Alice / Kei 发言；让 agent 自行从聊天推断并永久创建任务。

## UNKNOWN / NEEDS PROTOTYPE

- **UNKNOWN：** P1 是否必须跨重启保存 Conversation；产品规格仍为可选，架构 checkpoint 要求持久化，两者需用户 + Web 明确。
- **UNKNOWN：** 第一个 background feature 是 Reminder、background research 还是 memory consolidation；不同选择会改变最小 durability。
- **UNKNOWN：** Reminder 在 quiet hours、foreground busy、离线与多 channel 下的用户可见 policy。
- **UNKNOWN：** canceled / timed-out AgentScope call 遇到不可取消 blocking tool 时的最终副作用与恢复表现。
- **NEEDS PROTOTYPE：** AgentScope 2.0.3 state save、run cancellation、subagent completion 与 OpenAlice-owned result ledger 的对账。
- **NEEDS PROTOTYPE：** crash 发生在 result commit、Conversation append、external send 前后的 duplicate / unknown 行为。
- **NEEDS PRODUCT DECISION：** 哪个 Character 可呈现 specialist result，以及未参与角色可见的 provenance / relationship 范围。

## 来源与版本

全部访问于 2026-10-01。源码事实优先固定 commit；动态官方文档记录访问日期。

| 编号 | 来源 |
| :-- | :-- |
| S1 | OpenClaw `cd8907aa0c48ec74f5cc0c6f04873f50803902fb`：[Automation overview](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/docs/automation/index.md)、[How automations work](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/docs/automation/cron-jobs/how-it-works.md) |
| S2 | OpenClaw 同 revision：[Heartbeat](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/docs/gateway/heartbeat.md)、[Automation schedules](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/docs/automation/cron-jobs/schedules.md) |
| S3 | Quartz 2.5.x：[JobExecutionContext / fireInstanceId](https://www.quartz-scheduler.org/api/2.5.x/org/quartz/JobExecutionContext.html)、[Job / JobDetail tutorial](https://www.quartz-scheduler.org/documentation/quartz-2.5.x/tutorials/tutorial-lesson-03.html) |
| S4 | OpenClaw 同 revision：[Automation delivery](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/docs/automation/cron-jobs/delivery.md)、[`delivery-attempt-fence.ts`](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/src/cron/delivery-attempt-fence.ts)、[`service.restart-delivery.test.ts`](https://github.com/openclaw/openclaw/blob/cd8907aa0c48ec74f5cc0c6f04873f50803902fb/src/cron/service.restart-delivery.test.ts) |
| S5 | Temporal official：[What is Temporal?](https://docs.temporal.io/temporal)、[Tasks and Activity attempts](https://docs.temporal.io/tasks) |
| S6 | AgentScope Java `v2.0.3` / `e9721285c63a37c10b1d07aa540408e57ba56ab2`：[Agent / execution control](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/docs/v2/en/docs/building-blocks/agent.md)、[Context & AgentState](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/docs/v2/en/docs/building-blocks/context.md) |
| S7 | AgentScope 同 revision：[`ReActAgentCallFailurePersistenceTest`](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/agentscope-core/src/test/java/io/agentscope/core/agent/ReActAgentCallFailurePersistenceTest.java)、[`SubAgentToolCancellationTest`](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/agentscope-core/src/test/java/io/agentscope/core/tool/subagent/SubAgentToolCancellationTest.java) |
| S8 | AgentScope 同 revision：[`ReActAgentPerSessionStateTest`](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/agentscope-core/src/test/java/io/agentscope/core/agent/ReActAgentPerSessionStateTest.java) |
| S9 | [Eliciting Spoken Interruptions to Inform Proactive Speech Agent Design](https://arxiv.org/abs/2106.02077)（CHI 2022） |
| S10 | [Runtime Permissions for Privacy in Proactive Intelligent Assistants](https://www.usenix.org/conference/soups2022/presentation/malkin)（SOUPS 2022） |
| S11 | Spring Framework official：[Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)，source snapshot `f447f3c310009f5dc083306d25d12f13a1380dc4` |
| S12 | Quartz 2.5.x official：[JDBC JobStoreTX](https://www.quartz-scheduler.org/documentation/quartz-2.5.x/configuration/ConfigJobStoreTX.html)、[DisallowConcurrentExecution](https://www.quartz-scheduler.org/api/2.5.x/org/quartz/DisallowConcurrentExecution.html)、[Trigger misfire](https://www.javadoc.io/static/org.quartz-scheduler/quartz/2.5.2/org/quartz/Trigger.html)，repository snapshot `741ccc3cff96a32e4ab07d69624eaaa534624245` |
| S13 | AgentScope 同 revision：[Scheduler extension](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/docs/v2/en/integration/infrastructure/scheduler.md)、[`AgentQuartzJob`](https://github.com/agentscope-ai/agentscope-java/blob/e9721285c63a37c10b1d07aa540408e57ba56ab2/agentscope-extensions/agentscope-extensions-scheduler/agentscope-extensions-scheduler-quartz/src/main/java/io/agentscope/extensions/scheduler/quartz/AgentQuartzJob.java) |
| S14 | LangGraph official：[Functional API / durable execution and idempotency](https://docs.langchain.com/oss/python/langgraph/functional-api) |

Pare-Bench 2026 说明 proactive evaluation 至少应观察 goal inference、intervention timing 与 multi-app execution，但其模拟环境结果不直接决定 OpenAlice policy，故仅作为未来 evaluation 线索：[paper](https://arxiv.org/abs/2604.00842)。Hermes、Home Assistant 与 HomeRail 在本轮没有提供超出第一轮或上述来源的新边界证据，因此没有为增加来源数量重复展开。
