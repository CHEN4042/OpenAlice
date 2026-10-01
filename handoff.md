# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-01。
- 阶段：Architecture Re-initialization · Q&A checkpoint 与 Background / Task / Proactivity 第二轮独立研究已完成。
- 分支：`codex/architecture-research-20261001`；本轮继续沿用该 workstream，当前任务未授权 push。
- 产品规格已最小修改：补入 Alice 为主要角色、Kei 等角色参与长期主对话、用户参与控制，以及 Character / Specialist 的区别；P1 仍仅 Alice 基础聊天。
- Conversation Application、Character / Persona、Context Engine 与 Agent Runtime 的逻辑职责边界进一步收敛；Background / Task / Proactivity 仍为 OPEN。
- **当前仍没有 architecture authority**；dated notes 均为 WORKING NOTE / NON-AUTHORITATIVE。
- 未新增业务实现、工程骨架或数据库决策；`temp/` 继续仅作 reference。

## 本轮文档

- [架构讨论阶段快照](docs/research/2026-10-01-架构讨论阶段快照.md)。
- **Research report：** [Agent 架构关键问题调研](docs/research/2026-10-01-Agent架构关键问题调研.md)。
- **Second research report：** [Background / Task / Proactivity 第二轮调研](docs/research/2026-10-01-Background-Task-Proactivity-第二轮调研.md)。
- 更新 [Product Spec](docs/product/OpenAlice-产品规格说明.md)、[Source Policy](docs/research/source-policy.md)、[Web ↔ Codex 工作流](docs/bmad-workflow.md) 和 [文档地图](docs/index.md)。

## 关键 open questions / 风险

- Alice / Kei 的知情范围、共同经历与各自关系如何区分？
- Context、压缩、历史和执行状态由 OpenAlice 还是 AgentScope 拥有？具体 2.x artifact 与开关组合尚未实测。
- 前台输入、角色发言、提醒与后台完成如何排序、取消、去重与汇报？
- 第一个 background feature 尚未确定；Reminder、background research 与 memory consolidation 对 durability 的最低要求不同。
- AgentScope 2.0.3 的 call 中途 crash、blocking tool cancellation、subagent late completion 和 OpenAlice result ledger 对账仍需 prototype。
- P1 持久化目前存在产品可选与讨论要求的差异；原始记录保留、纠错/遗忘和 SQLite / PostgreSQL 仍 OPEN。
- 研究是静态源码/测试阅读；未运行外部项目测试。Hermes 固定 revision 复取失败，部分网站更新时间未知，报告已标明。

## 验证证据

- Phase A 与 Phase B 均执行 `git status --short`、`git diff` 和 `git diff --check`；检查通过。
- Ruby 内联 Markdown local-link 检查覆盖全仓库 Markdown：通过；第二轮报告 source reference 均有定义。
- Phase B 仅新增第二轮研究报告并更新文档地图与 handoff；未修改第一轮报告、产品权威、`temp/` 或 `docs/architecture/`。
- 敏感模式检查未发现 key、token、private key 或本机绝对路径；敏感本地配置内容未读取。
- 未运行 build / Maven / evaluation：本轮只有文档，仓库尚无新实现。

## 唯一下一步

Web review 两轮研究和真实 diff；用户 + Web 接受、修正或拒绝候选后，才把稳定结论写入 `docs/architecture/`。
