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

## Git 与 Codex 执行规范

本节是仓库 Git / Codex 协作的规范来源；其他文档只引用，不复制完整规则。

### Branch lifecycle 与命名

- 新 prompt、研究轮次或模型变更不自动产生新分支。同一 coherent workstream / reviewable change set 持续复用当前分支。
- 只有上一工作流已经 review 或完成，并开始真正独立的新工作流时，才创建新分支。不得仅为整理偏好而创建、重命名、切换、合并或删除分支。
- Codex 未来创建的 task branch 使用 `codex/<type>/<short-topic>`；`<short-topic>` 使用 lowercase kebab-case，日期只有在具有语义时才加入。
- 示例：`codex/research/background-task-proactivity`、`codex/docs/architecture-checkpoint`、`codex/feat/memory-persistence`、`codex/fix/context-assembly`、`codex/refactor/agent-runtime`、`codex/chore/repository-cleanup`。
- 现有 `codex/architecture-research-20261001` 分支保留原名，不重命名。

### Commit 与 push

- Commit 使用 `<type>(<scope>): <imperative English summary>`；允许的 type 为 `feat`、`fix`、`refactor`、`test`、`docs`、`research`、`chore`、`build`、`ci`。
- `research` 是 OpenAlice 对不建立产品或架构权威的证据/研究提交约定。例如：`docs(architecture): capture Q&A checkpoint`、`research(background): analyze task and proactivity architecture`。
- 每个 commit 必须逻辑内聚；不得把无关的仓库清理、研究、架构决定和实现混在一起。
- Codex 不从过去任务推断 commit 权限；只有当前任务明确授权或用户在当前任务确认后才 commit。
- 默认不 push；只有当前用户指令明确授权才 push，过去授权不延续到未来任务。除非用户明确要求且已审查后果，否则不得 force-push。

### Safety 与提交前检查

- `application-local.yml` 及任何真实 key、token、日志、构建产物和本机绝对路径不得提交、打印或导出。
- 保留用户已有变更；不得为清理 working tree 而使用 destructive reset、checkout、rebase、branch deletion 或 history rewriting。
- Commit 前检查 `git status --short`、`git diff` 和 `git diff --check`；commit 只能包含当前任务授权的文件。
