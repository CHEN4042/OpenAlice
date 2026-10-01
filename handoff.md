# OpenAlice Handoff

> 轻量交接：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前状态

- 日期：2026-10-01。
- 阶段：Architecture Re-initialization · 最新 Architecture Q&A checkpoint 已保存。
- 分支：`codex/architecture-research-20261001`；本轮继续沿用该 workstream，当前任务未授权 push。
- 产品规格已最小修改：补入 Alice 为主要角色、Kei 等角色参与长期主对话、用户参与控制，以及 Character / Specialist 的区别；P1 仍仅 Alice 基础聊天。
- Conversation Application、Character / Persona、Context Engine 与 Agent Runtime 的逻辑职责边界进一步收敛；Background / Task / Proactivity 仍为 OPEN。
- **当前仍没有 architecture authority**；dated notes 均为 WORKING NOTE / NON-AUTHORITATIVE。
- 未新增业务实现、工程骨架或数据库决策；`temp/` 继续仅作 reference。

## 本轮文档

- [架构讨论阶段快照](docs/research/2026-10-01-架构讨论阶段快照.md)。
- **Research report：** [Agent 架构关键问题调研](docs/research/2026-10-01-Agent架构关键问题调研.md)。
- 更新 [Product Spec](docs/product/OpenAlice-产品规格说明.md)、[Source Policy](docs/research/source-policy.md)、[Web ↔ Codex 工作流](docs/bmad-workflow.md) 和 [文档地图](docs/index.md)。

## 关键 open questions / 风险

- Alice / Kei 的知情范围、共同经历与各自关系如何区分？
- Context、压缩、历史和执行状态由 OpenAlice 还是 AgentScope 拥有？具体 2.x artifact 与开关组合尚未实测。
- 前台输入、角色发言、提醒与后台完成如何排序、取消、去重与汇报？
- P1 持久化目前存在产品可选与讨论要求的差异；原始记录保留、纠错/遗忘和 SQLite / PostgreSQL 仍 OPEN。
- 研究是静态源码/测试阅读；未运行外部项目测试。Hermes 固定 revision 复取失败，部分网站更新时间未知，报告已标明。

## 验证证据

- `git diff --check`：通过。
- `python3 -` 内联 Markdown 链接检查：覆盖 43 个文档（含 `temp/`），`MISSING_LINKS=0`、`NEW_MISSING_LINKS=0`、研究引用均有定义。
- `git status --short`：仅本轮 7 个 Markdown 文档；未引入 secret / token / 本机绝对路径，敏感本地配置仍 ignored。
- `git check-ignore application-local.yml`：仍 ignored；`git cat-file -e bd4618f^{commit}`：通过，历史可恢复；`git diff --name-only -- temp AGENTS.md`：无输出。
- 未运行 build / Maven / evaluation：本轮只有文档，仓库尚无新实现。

## 唯一下一步

执行第二轮 independent research：Background / Task / Proactivity Architecture；保持研究非权威，完成后交给 Web + 用户综合判断。
