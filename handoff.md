# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-03。
- 阶段：Repository Re-initialization · Phase 5 P1 Engineering Foundation 已在 `openalice-20261016` 实现并验证，等待 Cycle Review；完整 Conversation vertical slice 尚未开始。
- Java 21 单模块 Spring Boot 工程、SQLite/Flyway/JdbcClient、AgentRuntime、WebFlux SSE、自动化测试与 CI 已建立。

## 当前权威与输入

- 产品权威：[OpenAlice 产品规格说明](docs/product/OpenAlice-产品规格说明.md)。
- 架构权威：[Architecture Spine](docs/architecture/ARCHITECTURE-SPINE.md)。
- 当前计划：[Repository Re-initialization](docs/plans/active/repository-reinitialization.md)。
- Cycle 计划：[P1 Engineering Foundation](docs/plans/active/p1-engineering-foundation.md)。
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

## Foundation evidence

- 本地数据根由 `OPENALICE_HOME` 统一解析；默认数据库为 `~/.openalice/data/openalice.db`，测试使用临时目录。
- SQLite connections 启用 WAL、foreign keys 与 5000 ms busy timeout；Flyway V1 建立最小 `executions` table。
- `AgentScopeRuntime` 每个 Execution 新建 Bare `ReActAgent` 和 state store；默认测试使用 deterministic model，不需要 API key 或网络。
- `ExecutionCoordinator` 独立订阅 runtime 并发布 Reactor events；random-port HTTP test 证明 SSE client detach 后 Execution 继续完成。
- CI 使用 Java 21 执行 `./mvnw test`。没有 Architecture Amendment Candidate。

## 唯一下一步

Review the `openalice-20261016` Engineering Foundation Cycle and merge it into `main` through a reviewed PR before starting the Conversation vertical slice.
