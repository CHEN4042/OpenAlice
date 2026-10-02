# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-02。
- 阶段：Repository Re-initialization · Phase 4 与 AgentScope Technical Spike 已完成并 review；Phase 5 Engineering Foundation 是下一阶段，尚未开始。
- 当前仍没有业务实现或生产 Maven / Spring Boot 工程骨架；completed Spike source 已从 current tree 删除，可从 Git commit `0d5fd47` 恢复。

## 当前权威与输入

- 产品权威：[OpenAlice 产品规格说明](docs/product/OpenAlice-产品规格说明.md)。
- 架构权威：[Architecture Spine](docs/architecture/ARCHITECTURE-SPINE.md)。
- 当前计划：[Repository Re-initialization](docs/plans/active/repository-reinitialization.md)。
- 实验输入：[AgentScope Technical Spike](docs/research/2026-10-02-AgentScope-Technical-Spike.md)，状态为 EXPERIMENT EVIDENCE / NON-AUTHORITATIVE。
- [Architecture Synthesis Draft](docs/research/2026-10-01-Architecture-Synthesis-Draft.md)、[P1 Engineering Foundation 调研](docs/research/2026-10-02-P1-Engineering-Foundation-调研.md) 与其他 [research](docs/research/) 继续是非权威证据。

## 当前架构摘要

- P1 是 single Spring Boot process，使用 SQLite、ordinary HTTP + SSE 与显式 Execution cancellation boundary。
- OpenAlice 拥有产品语义和 durable facts；Conversation Application 独占 Timeline write；AgentScope 位于可替换 runtime boundary 后。
- Conversation / Message / Turn / Execution 保持独立 durable identity；stale active Execution 在 restart reconciliation 后成为 `INTERRUPTED`。
- Retrieval index 是 derived / rebuildable data，不是 Conversation 或 Memory source of truth。
- P1 Agent Runtime 默认使用 Bare AgentScope core `ReActAgent`，位于 OpenAlice-owned Agent Runtime adapter 后；runtime state 按 Execution 隔离。

## 已接受边界与延后

- OpenAlice 持有 durable lifecycle、cancellation fact、late-result gate 与 Timeline commit；AgentScope interrupt 是 best-effort，SSE response lifecycle 与 Execution lifecycle 解耦。
- **DEFERRED：** advanced / semantic Memory、vector backend、PostgreSQL migration、Background / Proactivity、WebSocket / Voice、device / edge 与 distributed runtime。
- Repository Re-initialization 尚未完成；`temp/` 保留到计划中的 cleanup phase。

## 唯一下一步

Define and execute the minimal P1 Engineering Foundation based on the accepted Architecture Authority and AgentScope runtime boundary.
