# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-01。
- 阶段：Architecture Re-initialization · Architecture Synthesis Draft 等待 User + Web review。
- 文档与架构阶段直接在 `main` 工作；当前仍没有 architecture authority，也没有业务实现或工程骨架。

## 当前权威与输入

- 产品权威：[OpenAlice 产品规格说明](docs/product/OpenAlice-产品规格说明.md)。
- 当前计划：[Repository Re-initialization](docs/plans/active/repository-reinitialization.md)。
- 架构权威：尚未建立。
- 待 review 综合稿：[Architecture Synthesis Draft](docs/research/2026-10-01-Architecture-Synthesis-Draft.md)，状态为 DRAFT / NON-AUTHORITATIVE。
- 研究输入：[docs/research/](docs/research/)。

## 已接受约束

- P1 长期主 Conversation 必须跨应用与机器重启持久化；存储是事实来源，durable history 与 Prompt Context 分离。
- OpenAlice 拥有 Conversation、Character、Persona、Memory、Context admission、Task、Result / Delivery 与 product policy；AgentScope 提供可替换执行机制。
- Conversation Application 是主时间线唯一 owner。
- `execution completed`、`interrupt the user` 与 `character speaks` 是三个不同决定。

## 候选与待验证内容

- **CANDIDATE：** Character Presence / Turn Audience / Memory Visibility / Relationship Ownership 四层模型。
- **CANDIDATE：** Context Engine pipeline、轻量 execution coordination 与 background result-first chain。
- **NEEDS PROTOTYPE：** bare `ReActAgent` 与 Selective Harness 对比，覆盖多轮/context ownership、慢工具取消、crash/restart、subagent completion。
- **OPEN：** 存储与 Schema、execution 状态机、角色记忆治理、首个 background feature 与 delivery policy。

## 唯一下一步

User + Web review [Architecture Synthesis Draft](docs/research/2026-10-01-Architecture-Synthesis-Draft.md)，决定哪些结论进入首版 architecture authority。
