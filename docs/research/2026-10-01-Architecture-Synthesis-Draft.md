# OpenAlice Architecture Synthesis Draft

> **状态：DRAFT / NON-AUTHORITATIVE**<br>
> **日期：2026-10-01**<br>
> **用途：供 User + Web 进行架构综合 review。本文不是 `docs/architecture/` 权威，不授权业务实现。**

## 1. 输入与状态标记

本文综合以下输入：

- [OpenAlice 产品规格说明](../product/OpenAlice-产品规格说明.md)
- [架构讨论阶段快照](2026-10-01-架构讨论阶段快照.md)
- [Agent 架构关键问题调研](2026-10-01-Agent架构关键问题调研.md)
- [Background / Task / Proactivity 第二轮调研](2026-10-01-Background-Task-Proactivity-第二轮调研.md)

本文使用五种状态：

| 状态 | 含义 |
| :-- | :-- |
| **ACCEPTED** | 用户已接受的产品或 ownership 约束，可以作为后续 architecture authority 的输入 |
| **CANDIDATE** | 已形成较完整模型，但仍需 User + Web review |
| **OPEN** | 尚未决定，需要进一步产品或架构判断 |
| **NEEDS PROTOTYPE** | 仅靠文档与源码阅读不足，必须用最小技术实验验证 |
| **DEFERRED** | 当前阶段明确不实现或不决定 |

## 2. 产品约束

以下约束优先于框架便利性：

- OpenAlice 面向单用户、长期个性化使用，Alice 是主要角色。
- 用户界面呈现一条长期连续的主 Conversation；多 Client 连接同一个中心 Runtime。
- Character Agent 是用户可见角色；Specialist Agent 为隔离任务服务，不默认成为主对话角色。
- 产品语义必须由 OpenAlice 拥有，不能隐含在某个 AgentScope session、prompt string 或运行中对象里。
- P1 只要求 Alice 的基础聊天产品闭环以及长期主 Conversation 的可靠持久化。

## 3. P1 Conversation Persistence（ACCEPTED）

唯一的长期主 Conversation 是 OpenAlice 的持久化产品数据，必须跨应用重启和机器重启保留。持久化存储是事实来源；Agent Runtime 的 session 与进程内对象只能是执行状态或派生缓存。

Durable Conversation 与 Prompt Context 分离：

- UI 可以加载近期消息并分页读取旧消息；
- 模型可以接收近期消息、摘要、相关 Memory 与当前 Turn；
- 持久化全部历史不意味着每次模型调用都注入全部历史。

P1 不要求恢复崩溃前的 token 流。一次 execution 中断后，可以保留用户消息和失败 execution，让 assistant 消息缺失或标记为不完整；系统不能伪造已完成回复。数据库、Schema、长期保留与删除策略仍 **OPEN**。向量 Memory、遗忘策略和高级 consolidation 属于 **DEFERRED**。

## 4. Conversation Application（ACCEPTED）

Conversation Application 是用户 interaction / turn 的产品级 orchestration owner，也是主 Conversation 时间线的唯一写入 owner：

```text
user input → establish turn → build context → invoke execution
    → receive result → decide visible outcome → persist timeline outcome
```

它拥有 Conversation、Message、Turn 与 Timeline 语义，但不实现 LLM provider、ReAct loop、vector retrieval、scheduler 或通用 workflow engine。Runtime、background completion 和 proactive event 都必须把结果交回这一产品边界，由它决定是否以及如何写入主时间线。

## 5. Character / Persona

### 5.1 Identity ownership（CANDIDATE）

Character identity、Conversation identity 与 Execution identity 相互独立。Persona 是稳定、版本化的产品输入；Agent instance、model instance 或一段 prompt 不能单独定义“Alice 是谁”。每个用户可见角色可以有自己的 Persona 与 relationship context。

### 5.2 Presence / Audience（CANDIDATE）

多角色对话采用四层候选模型：

```text
Presence → Turn Audience → Memory Visibility → Relationship Ownership
```

- **Presence**：角色当前是否在场；Speaking / Participation 单独决定是否以及如何发言。角色可以是 `present = true, participation = PASSIVE`。
- **Turn Audience**：当前消息面向谁。`@Alice` 默认选择 responder，不自动对其他在场角色隐藏；明确的“只告诉 Alice”才收窄 audience。
- **Memory Visibility**：角色后续能读取什么。新角色不自动获得全部原始历史；离场角色停止见证新消息，但保留离场前已经见证的内容。
- **Relationship Ownership**：共享事实不等于共享关系记忆；每个角色可以形成自己的关系语义。

移出 Presence 与删除角色、身份或既有记忆是不同操作。被动见证与主动参与发言也必须分开。

## 6. Memory ownership（CANDIDATE）

OpenAlice 拥有 Memory 的产品语义、来源、可见性、关系归属、纠错与删除意图。Memory 系统可以派生摘要或长期事实，但不能把派生内容伪装成用户原话，也不能用 runtime session 代替持久化事实。

Raw Conversation、Conversation summary、Stable Profile、Episodic Memory、Relationship Memory 需要保留概念区别。具体分层、检索、冲突合并、保留期、向量存储与 consolidation 算法仍 **OPEN / DEFERRED**。

## 7. Context Engine（CANDIDATE）

候选 pipeline 是：

```text
Context Sources → Context Resolution → Context Admission
    → Context View → Prompt Composer → Agent Runtime
```

Context Engine 不拥有 Conversation 或 Memory 原始数据。它把候选来源解析为某次 execution 可见的 Context View，并保留 provenance、visibility、target、fact / derived distinction、version 与 freshness。Retrieval result 只是 candidate context，不自动进入 prompt，也不伪装成 User Message 写回 Conversation。

Character Agent 与 Specialist Agent 可以获得不同 Context View。Context Bundle 与 Prompt Plan 逻辑分离，每次执行必须有明确预算。

## 8. Agent Runtime 与 AgentScope

### 8.1 Ownership boundary（ACCEPTED）

OpenAlice 拥有 Conversation、Character、Persona、User Profile、Memory、Context admission、product policy、Task、Result / Delivery 与持久化 execution 语义。AgentScope 提供 model invocation、ReAct、tool、streaming、cancellation/interruption、specialist/subagent 与 selected execution state 等可替换执行机制。

框架提供的 session、memory、context 或 compaction 不能自动成为 OpenAlice 产品事实来源。

### 8.2 Selective Harness（NEEDS PROTOTYPE）

当前假设是采用 Selective Harness：只使用经验证能够服从 OpenAlice ownership 与 lifecycle 语义的 Harness 能力。不能根据 API 外观直接决定全量采用 Harness，也不能在没有实验时决定完全绕过。

Technical Spike 需要比较 bare `ReActAgent` 与 selective Harness：

| 场景 | 必须验证的问题 |
| :-- | :-- |
| 多轮与 context ownership | OpenAlice 能否提供并重建上下文，Harness 是否静默持有或改写产品状态 |
| 慢工具取消 | 取消能否传播；迟到结果如何识别、记录和抑制交付 |
| crash / restart | OpenAlice 能否依据自身持久化记录解释并恢复 execution 状态 |
| subagent completion | 结果能否回到明确 parent execution，避免直接污染主 Conversation |

通过标准是 OpenAlice 的事实记录足以恢复和对账，并且 runtime 不会改变 Conversation、Audience、Memory 或 Character ownership。Spike 只产出证据与边界结论，不应顺带创建业务骨架。

## 9. Execution coordination（CANDIDATE）

即使 P1 是单用户、单进程，也需要轻量 coordination 概念处理 foreground ordering、cancellation、重复提交以及未来 background collision。它是逻辑职责，不等于分布式队列、Gateway service、Worker service、通用 Event Bus 或完整 Task Engine。

Execution 应具有可关联的 identity 与状态；Conversation outcome、execution result 和 delivery state 不能混成一个布尔完成标记。具体状态机和持久化 Schema 仍 **OPEN**。

## 10. Background / Task / Proactivity（CANDIDATE，P1 后）

现有证据不支持先建立一个包揽提醒、后台研究、Agent completion 与 Memory consolidation 的通用 Task Engine。更清晰的分析链是：

```text
Task Definition → Trigger → Occurrence / Run
    → Execution / Attempt → Result → Delivery
```

- Background 产生事实与 Result；
- Proactivity 判断是否以及何时打断用户；
- Character 决定最终如何表达；
- Conversation Application 决定是否写入主时间线。

必须保持：

```text
execution completed ≠ interrupt the user ≠ character speaks
```

采用 result-first 路径：结果先进入 OpenAlice 拥有、可去重和对账的记录，再做 delivery policy、主动通知与角色表达。Memory consolidation 可以静默完成。第一个 background feature、durability 等级、调度器与 delivery policy 仍 **OPEN**，整体 **DEFERRED** 到 P1 之后。

## 11. P1 边界汇总

| 分类 | 当前内容 |
| :-- | :-- |
| **ACCEPTED** | Alice 基础聊天产品闭环；一条长期主 Conversation；跨应用与机器重启持久化；Conversation Application 是用户可见时间线唯一 owner；OpenAlice 拥有产品语义，AgentScope 提供执行机制 |
| **CANDIDATE** | Character identity；Presence / Audience 四层模型；Context pipeline；轻量 execution coordination；background result-first chain |
| **NEEDS PROTOTYPE** | bare `ReActAgent` 与 Selective Harness 对比；取消、crash/restart、subagent completion |
| **OPEN** | 存储与 Schema；execution 状态机；角色删除与记忆治理细节；首个 background feature |
| **DEFERRED** | Kei 实现、多角色 UI、高级 Memory、Background / Proactivity、通用任务基础设施、分布式部署 |

## 12. Review checklist

User + Web review 需要明确：

1. Conversation Application 的职责边界是否准确表达了主时间线唯一 owner 这一已接受约束。
2. 是否接受 Character Presence / Audience 四层模型，尤其是 `@角色` 与私密 audience 的区别。
3. 是否接受 Context Engine pipeline 与 OpenAlice / AgentScope ownership boundary。
4. Technical Spike 的比较范围与通过标准是否足够约束后续实现。
5. Background 的 result-first chain 与三项不变量是否应进入 architecture authority。
6. 哪些候选应写入首版 `docs/architecture/`，哪些继续保持研究状态。
