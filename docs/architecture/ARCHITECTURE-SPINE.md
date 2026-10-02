# OpenAlice Architecture Spine

> **状态：ACTIVE — 当前架构权威**<br>
> **日期：2026-10-02**<br>
> **范围：** 系统形态、产品语义所有权、P1 技术默认、验证门、延后项与迁移触发条件<br>
> **不包含：** 物理数据库 Schema、package / Maven module 布局、具体 HTTP URL、ORM mapping、实现类或 prototype 结果<br>
> **产品权威：** [OpenAlice 产品规格说明](../product/OpenAlice-产品规格说明.md)<br>
> **研究证据：** [Architecture Synthesis Draft](../research/2026-10-01-Architecture-Synthesis-Draft.md)、[P1 Engineering Foundation 调研](../research/2026-10-02-P1-Engineering-Foundation-调研.md) 及 [docs/research/](../research/)

本文是 OpenAlice 当前唯一的架构权威入口。实现、Schema、测试与后续决策必须服从本文；研究材料保留证据价值，但不能覆盖本文。

## 1. Authority boundary

本架构使用四类结论：

- **Invariant**：跨实现必须保持的语义和所有权；框架便利不能改变它。
- **P1 Default**：当前实现周期的默认技术选择；未来只能在触发条件出现并完成 review 后变更。
- **NEEDS PROTOTYPE**：实现前必须用有边界实验验证的 runtime integration。
- **DEFERRED**：明确不进入 P1，不为其预建基础设施。

稳定性层级为：

```text
Product Authority
→ Architecture Authority
→ Active Plan
→ Code / Schema / Tests / Evaluations
→ Handoff
→ Research Evidence
```

## 2. System shape

OpenAlice 采用下述逻辑形态：

```text
Clients
   ↓
Interaction Boundary
   ↓
Conversation Application
   ├── Character / Persona
   ├── Context Engine
   ├── Memory
   └── Execution Coordination
            ↓
       Agent Runtime
            ↓
        AgentScope
```

### P1 physical deployment

```text
single Spring Boot process
```

逻辑能力边界不等于物理 service、Maven module 或独立进程。P1 不引入 microservices、Gateway service、Worker service、Kafka / RabbitMQ、distributed scheduler、generic workflow engine 或 global Event Bus。同一进程内同步协调默认使用普通 Java 调用；只有出现真实异步失败边界时才增加相应机制。

## 3. Ownership and dependency direction

> **Invariant：OpenAlice owns product semantics. AgentScope provides replaceable execution mechanisms.**

OpenAlice 拥有：

- Conversation、Message、Turn 与用户可见 Timeline；
- Character identity、Persona、User Profile 与 relationship semantics；
- Memory semantics、provenance、visibility 与 lifecycle intent；
- Context resolution / admission 与 product policy；
- Task、Result、Delivery 与 execution 的持久产品事实；
- 哪个 candidate result 可以成为用户可见 outcome 的最终决定。

Agent Runtime / AgentScope 可以提供：

- model invocation 与 ReAct loop；
- Tools、streaming、cancellation 与 execution events；
- specialist / subagent execution；
- 经 prototype 接受的 selected runtime state。

AgentScope session、AgentState、memory、context 或 compaction 不得静默成为 OpenAlice product truth。OpenAlice 必须能在替换 runtime mechanism 后保留产品身份、Conversation、Memory meaning 与 durable lifecycle。

## 4. Conversation and Timeline

OpenAlice 有一条长期运行的 main Conversation。它是持久产品数据，必须跨正常应用重启和机器重启保留。OpenAlice persistent storage 是 source of truth；AgentScope session / AgentState 不是 Conversation source of truth。

Durable Conversation 与 per-turn Prompt Context 是不同概念。UI 可以先加载近期 Message 并分页读取旧内容；模型只接收本 Turn 所需的 recent Conversation、selected summary、selected Memory 与 current Turn。持久化全部历史不表示每次 prompt 包含全部历史。

Conversation Application 是用户可见 Timeline 的唯一 write owner：

```text
user input
→ establish Turn
→ persist durable input
→ build Context View
→ invoke Character / Agent execution
→ receive candidate result
→ decide visible outcome
→ commit Timeline outcome
```

Runtime completion、Tool result、subagent completion 或 background result 都不能直接写用户可见 Message。它们先成为 candidate result 或 durable fact，再由 Conversation Application 按产品语义提交。

## 5. Durable semantic identities

P1 保留四个独立的 durable identity：

| Concept | Architecture meaning |
| :-- | :-- |
| **Conversation** | 长期产品 interaction container 与 Timeline boundary |
| **Message** | 已提交到 Timeline 的用户可见 statement |
| **Turn** | 一次 user interaction / causal request |
| **Execution** | 为完成某个 Turn 发起的一次 runtime attempt |

必须保持：

```text
Turn ≠ Execution
Conversation ≠ AgentScope session
Message ≠ runtime event
```

这些是 semantic identity，不要求四张物理 table。Schema、ID format、ORM mapping 与 repository implementation 在实现阶段决定，但不得合并或丢失上述因果边界。

## 6. Execution lifecycle

### P1 conceptual states

```text
RUNNING
COMPLETED
FAILED
CANCELLED
INTERRUPTED
```

- **RUNNING**：OpenAlice 已接受 Execution 进入 active lifecycle；不保证某个 JVM thread 此刻仍存活。
- **COMPLETED**：有效的 final assistant outcome 已被 Conversation Application 接受并提交。
- **FAILED**：OpenAlice 已观察到明确的 execution failure。
- **CANCELLED**：用户或系统 cancellation 已成为接受的 terminal product outcome。
- **INTERRUPTED**：restart / reconciliation 后发现先前 active Execution 没有可信 completion record。

P1 不引入 `PENDING`、`QUEUED`、`RETRYING`、`PAUSED` 或 `CANCELLING`。只有出现真实 queue、retry、pause 或 admission 产品需求时，才能扩展状态模型。

### Crash and restart invariant

```text
persist User Message
→ create durable Turn / Execution
→ runtime begins
→ process crashes
→ restart
→ stale active Execution becomes INTERRUPTED
```

User Message 必须保留。系统不得伪造 completed Alice response，也不得用 AgentScope session presence 推断完成。P1 不要求 token-level resume 或 runtime rewind。

Partial streamed token 默认是 ephemeral presentation，不自动成为 durable committed Message。正常 Assistant Message 只有在完整 outcome 被接受时才能提交；迟到 result 必须服从已持久化 terminal transition。

### Cancellation invariant

Cancellation 以 Execution identity 为目标，是显式 product command。OpenAlice 记录 cancellation outcome / intent，再请求 Agent Runtime 停止。SSE 连接关闭、browser refresh 或 client network loss 都不能定义 Execution lifecycle。

## 7. Character and Persona

必须保持：

```text
Character Identity
≠ Conversation Identity
≠ Execution Identity
```

Persona 是 stable、versioned product input。AgentScope agent instance、model instance 或 prompt string 都不能单独定义 Alice identity。

- **Character Agent**：用户可见，拥有 Persona 与 relationship participation，可以由 Conversation Application 提交角色发言。
- **Specialist Agent**：执行隔离任务并返回 result；它的 output 不自动成为 main Conversation Message，也不自动获得完整 Alice Persona 或私人关系上下文。

### Presence and audience

多角色语义分为四层：

```text
Conversation Presence
→ Turn Audience
→ Memory Visibility
→ Relationship Ownership
```

- Presence 与 speaking / participation 分离；Character 可以 present 但 passive。
- `@Alice` 选择 responder，不自动对其他 present Character 隐藏 Turn；明确的 private wording 才能收窄 Turn Audience。
- 新加入 Character 不自动获得加入前的完整 raw Conversation。
- 从 active group 移除后，Character 停止见证未来 group-visible Conversation；移除不自动删除它已经保留的 Memory。
- Shared fact 不自动成为 shared Relationship Memory；relationship meaning 保持 Character-specific，除非产品明确共享。
- Passive witness 可以保留其见证内容，但不得声称自己主动参与了事件。

P1 只实现 Alice。Kei、多角色执行和多角色 UI 均为 deferred；上述不变量用于防止 P1 数据与语义阻塞未来演进。

## 8. Context Engine

Context Engine 使用下述 conceptual pipeline：

```text
Context Sources
→ Context Resolution
→ Context Admission
→ Context View
→ Prompt Composer
→ Agent Runtime
```

Context Engine 不拥有 source Conversation 或 raw Memory。Context material 在相关时保留 provenance/source、visibility、target Character/Agent、fact / derived distinction、version 与 freshness。

必须保持：

```text
Retrieval ≠ Prompt Injection
```

Retrieved Memory、RAG、project 或 runtime context 先成为 candidate context，经过 admission 后才进入特定 execution 的 Context View。系统不得把 retrieved / derived information 伪造成 User Message 写回 Conversation。

Character Agent 与 Specialist Agent 可以得到不同 Context View。Context Bundle（可用候选信息）与 Prompt Plan（本次实际提供什么）在概念上分离，并受明确 context budget 约束。

## 9. Memory and retrieval boundary

OpenAlice 拥有 Memory meaning、provenance、visibility、relationship ownership、纠错与删除 intent。具体高级 Memory lifecycle、consolidation 与 retrieval algorithm 不进入 P1。

> **Invariant：Retrieval indexes are derived, rebuildable data. They are not the source of truth for Conversation or Memory.**

```text
Canonical Product Data
        ↓
      SQLite
        ↓
   Retrieval Layer
     ├── lexical index
     └── semantic index
```

P1 可以使用普通 query，并在出现明确 lexical search 需求时评估 FTS5。未来 semantic retrieval 可以评估 sqlite-vec、其他 local vector index、PostgreSQL + pgvector 或合适的其他 backend；当前不选择 vector implementation。

Embedding model、index format 或 retrieval backend 的改变不得改变 canonical Memory meaning，也不得要求重写 Conversation / Message / Turn / Execution 产品语义。Index 必须可以从 canonical data 重建。

## 10. P1 persistence default

```text
P1 canonical persistence default = SQLite
```

这是 P1 infrastructure default，不是永久 product invariant。选择依据是单用户、单 Spring Boot process、MacBook 开发、Mac mini 单节点部署、低写并发与低运维负担。

实现必须保护以下边界：

- SQLite-specific SQL、rowid、JSON/FTS operator、PRAGMA 与 backup mechanism 不进入 Product/Application semantics；
- 使用稳定 product identity、causal link、provenance 与可迁移时间语义；
- LLM / Tool execution 不持有长数据库 transaction；
- database file 位于 backend 所在设备的 local filesystem；
- migration、backup 与 restore 是 Engineering Foundation 的验证范围。

具体 Schema、JDBC library、migration library、PRAGMA、connection pool 与 backup schedule 属于实现决策。

### PostgreSQL migration policy

Semantic / vector retrieval 本身不是 migration trigger。只有出现并证实以下真实需要时，才重新评估 PostgreSQL：

- sustained concurrent write pressure；
- 多个 backend process 需要 shared database access；
- SQLite locking 已成为 observed bottleneck；
- dataset / query complexity 明确超出 SQLite suitability；
- PostgreSQL-specific capability 带来实质收益；
- operational requirement 足以证明运行 database server 的成本合理。

PostgreSQL / pgvector 是 deferred infrastructure option。迁移不得改变 Conversation、Message、Turn、Execution 或 Memory product semantics。

## 11. P1 client transport

```text
ordinary HTTP commands / queries
+
server-to-client SSE streaming
```

P1 不要求 WebSocket。发送 user input、查询 durable state 与显式 cancellation 使用 ordinary HTTP；assistant output / execution status 使用 SSE 向 client streaming。

必须保持：

```text
SSE disconnect ≠ Execution cancellation
```

Browser refresh 或 network loss 后，durable Conversation 与 Execution state 是恢复来源。P1 不要求 durable token replay 或 durable `Last-Event-ID` replay。

Transport-independent event semantics 可以包括：

- execution accepted / started；
- assistant text delta；
- completed；
- failed；
- cancelled；
- optional heartbeat。

Assistant text delta 默认是 ephemeral presentation。Terminal product state 必须先提交，再把 terminal presentation 当作 authoritative notification。

Architecture 不锁定 `SseEmitter`、MVC reactive return、WebFlux 或 Reactor adapter code。具体 adapter 由 prototype / implementation 选择，但不能改变 transport 与 lifecycle invariants。

## 12. Execution coordination

Execution Coordination 是单进程内的轻量 logical capability，用于：

- foreground Turn ordering；
- cancellation；
- duplicate submission；
- stale / late completion；
- future foreground / background collision。

它不意味着 queue infrastructure、workflow platform、distributed coordinator 或 generic Task Engine。P1 优先用进程内 coordination 与 durable terminal transition；只有真实失败边界出现后才增加机制。

## 13. Background, Task and Proactivity

概念链为：

```text
Task Definition
→ Trigger
→ Occurrence / Run
→ Execution / Attempt
→ Result
→ Delivery
```

必须保持：

```text
execution completed
≠ interrupt the user
≠ character speaks
```

Architecture principle：Background 产生 facts/results；Proactivity 决定是否以及何时打断用户；Character 决定表达方式。Durable Result 应先于 optional Conversation presentation，Conversation Application 仍是 user-visible Timeline writer。

Background scheduler、Reminder、proactive interaction 与 generic Task Engine 不进入 P1。本文只保护未来所需的语义分离，不预建实现。

## 14. P1 scope

### Included

- Backend 与 Web UI；
- real model execution；
- Alice basic multi-turn chat；
- durable permanent Conversation；
- ordinary HTTP + SSE；
- explicit Execution cancellation boundary；
- minimum durable Conversation / Message / Turn / Execution semantics；
- SQLite persistence；
- AgentScope behind a replaceable Agent Runtime boundary。

### Not required

- Kei implementation 或 multi-character UI；
- advanced Memory、semantic/vector retrieval 或 Memory consolidation；
- proactive conversation、Reminder 或 background task platform；
- PostgreSQL / pgvector；
- WebSocket、realtime Voice 或 presence protocol；
- multimodal、device / edge protocol；
- distributed deployment。

## 15. NEEDS PROTOTYPE

在进入正式 runtime integration 前，运行一个有边界 AgentScope Technical Spike，比较：

```text
Bare ReActAgent
vs
Selective HarnessAgent
```

Spike 必须验证：

- multi-turn / context ownership 与 duplicate history risk；
- Harness memory / context / compaction hooks 能否选择性关闭；
- deliberately slow Tool cancellation；
- SSE disconnect / timeout 与 runtime subscription 的关系；
- hard process termination 与 restart reconciliation；
- late Tool / subagent result；
- subagent completion 与 cancellation propagation；
- OpenAlice durable state 与 AgentScope AgentState 的对账。

通过标准是：OpenAlice 可以仅依据自己的 durable facts 解释 Conversation 与 Execution 状态，AgentScope 不会静默改写 product ownership；disconnect 不会自动改变 Execution outcome；late result 不能越过 OpenAlice terminal transition。

Spike 决定复用多少 AgentScope capability，不重新讨论 OpenAlice product ownership，也不顺带建立 application foundation。

## 16. DEFERRED

- semantic Memory implementation 与 advanced Memory governance；
- sqlite-vec / pgvector / vector backend 选择；
- PostgreSQL migration；
- Memory consolidation cadence；
- background scheduler、Reminder 与 proactive interaction；
- WebSocket / realtime transport、Voice 与 multimodal；
- device / edge protocol；
- distributed runtime、broker、microservices 与 workflow engine。

Deferred 不是未来承诺，也不授权空 abstraction、placeholder module 或提前引入 dependency。每项能力只有在 Product Authority 与 active plan 给出真实需求后才能开始设计。

## 17. Future change gates

对本文的稳定变更必须：

1. 由产品需求、prototype evidence 或实现验证提出明确 trigger；
2. 说明是否改变 Product Authority、architecture invariant、P1 default 或 deferred boundary；
3. 保留 migration / rollback 路径与验证方式；
4. 经 User + Web 接受后更新本文；
5. 必要时再建立有单一职责的 ADR，而不是让 chat、handoff 或 research note 成为隐式 authority。

实现中的 convenience、框架默认值或模型更换都不能单独构成架构变更理由。
