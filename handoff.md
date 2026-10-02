# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-02。
- 阶段：Repository Re-initialization · Phase 4 Architecture Re-initialization 已完成；Phase 5 Engineering Foundation 尚未开始。
- 首版 Architecture Authority 与 bounded AgentScope Technical Spike 已完成；当前仍没有业务实现或生产 Maven / Spring Boot 工程骨架。

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

## Spike 结论与延后

- **实验建议：** P1 使用 Bare `ReActAgent`；OpenAlice 每次 Execution 显式提供 Context View、持有 durable lifecycle 与 late-result gate，并把 SSE response lifecycle 与 runtime lifecycle 解耦。该建议等待 User + Web review，尚未提升为 Architecture Authority。
- **DEFERRED：** advanced / semantic Memory、vector backend、PostgreSQL migration、Background / Proactivity、WebSocket / Voice、device / edge 与 distributed runtime。
- Repository Re-initialization 尚未完成；`temp/` 保留到计划中的 cleanup phase。

## 唯一下一步

User + Web review AgentScope Technical Spike evidence and decide the runtime integration boundary before Engineering Foundation.
