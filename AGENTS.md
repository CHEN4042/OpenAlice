# AGENTS.md · OpenAlice

> 当前阶段：**Repository Re-initialization**。业务实现暂停；先建立产品、架构、评测和工程协作基础。

## 开工顺序

1. 阅读本文件和 [handoff.md](handoff.md)。
2. 阅读 [docs/index.md](docs/index.md) 与 [当前活动计划](docs/plans/active/repository-reinitialization.md)。
3. 需要产品或工程历史时，只读取 `temp/`，并将其视为 reference，不视为 authority。
4. 只有 `docs/product/` 和 `docs/architecture/` 出现明确的 current 文档后，才能把它们作为相应领域的权威。

## 当前边界

- 不开始新的业务功能，不创建替代 `src/`、Maven 文件或工程骨架，不从旧 package、interface、database model 或 framework choice 推导新设计。
- legacy implementation、`pom.xml`、`compose.yaml` 和旧 `web/` 已从 active repository 移除；需要追溯时只通过 Git 历史或已推送的 `bd4618f` 查看。
- `temp/` 只包含旧文档和研究材料，不包含可运行的旧实现。
- 产品重新澄清完成前，不把 `temp/product/` 中的旧 SPEC、需求书、人格规则或 Forge 结果当成当前产品契约。
- 架构重新设计完成前，不把 `temp/engineering/` 中的旧 spine、ADR、技术研究或代码导览当成工程决策。
- 不为了“看起来完整”创建空模块、空接口、provider、multi-tenant 或高并发抽象。

## 权威与文件职责

权威关系从稳定到易变依次为：

1. `docs/product/`：当前产品行为契约；
2. `docs/architecture/`：当前架构不变量与已接受决策；
3. `docs/plans/active/`：本轮执行范围、步骤和验收；
4. 代码、schema、测试和 evaluation：实现与验证证据；
5. `handoff.md`：当前状态、风险和唯一下一步；
6. `docs/research/`、`temp/`：研究输入和历史追溯。

`AGENTS.md` 只定义导航和协作规则，不复制完整产品或架构内容。任何稳定决策都必须写入对应权威文档，不能只留在聊天或 handoff 中。

## 协作与验证

- Planner 负责范围、契约影响、验收和风险；Builder 只实现已批准的 contract 与 architecture；Reviewer 必须读取真实 diff 和验证证据。
- 多 Agent 只在文件所有权和验收边界清晰时并行；禁止同时修改同一权威文档或实现边界。
- 每轮结束更新 handoff，记录精确命令、结果、风险和唯一下一步。
- 结构变化必须检查文件存在、仓库内部链接、Git 可恢复性和敏感文件；业务实现阶段再加入构建、测试、CI 和 evaluation 门禁。

## 安全与 Git

- `application-local.yml` 及任何真实 key、token、日志、构建产物和本机绝对路径不得提交、打印或导出。
- 修改前保留用户已有变更；不使用 destructive reset 或 checkout 覆盖工作。
- commit 只在用户明确指示或 review 确认后执行；push 一律由用户执行。
- 新分支默认使用 `codex/` 前缀；commit 遵循 Conventional Commits：`<type>(<scope>): <中文 subject>`。
