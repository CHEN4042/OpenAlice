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

### 当前文档阶段

- Product definition、Architecture discussion、Research、Architecture synthesis 和纯文档仓库维护直接在 `main` 上进行；Git commit 提供历史与回滚。
- 不因新 research question、prompt、model change、Architecture Q&A、Markdown 或 handoff 更新创建分支。
- 直接提交文档不授予 Codex 产品或架构决策权。权威流程始终是：`Research → User + Web discussion → Candidate Decision → User acceptance → authoritative Product / Architecture document`。
- Codex 不得把自己的 research 自动提升为 authority。

### 未来实现 Cycle

- 真实可执行实现开始后，使用 `openalice-YYYYMMDD` Cycle branch；无 `/`、无 `codex-` 前缀，默认不加 topic / feature suffix，日期表示计划 checkpoint / closing period。
- 一个 Cycle 通常约 1–2 周；早期可含约 1–3 个相关目标，后期通常聚焦约 1–2 项有意义的能力。小幅延期不要求重命名。
- Cycle 名称由 User + Web 决定；Codex 不自行创建、发明或重命名 Cycle branch。特殊 experiment branch 也必须由 User + Web 明确指示。
- 已明确分配的 active Cycle 内，Codex 通常可 edit、validate、commit 并 push 当前 Cycle branch；默认不再创建 feature branch。
- Cycle 以 small coherent commits 推进，可按需做中途 Web review；Cycle Review 后通过一个 PR 进入 `main`，完成 final checks / review 后使用 Merge Commit，并删除已完成 Cycle branch。
- 实现阶段 Codex 不得直接 push 或 merge `main`。进入实现阶段后再为 `main` 配置 PR、review 与 checks 保护；本阶段不配置。

### Commit 与 push

- Commit 使用 `<type>: <中文简短说明>`；常用 type 为 `feat`、`fix`、`docs`、`test`、`refactor`、`chore`，不要求 scope，不使用自定义 `research` type。
- 有意义的 commit 用中文 body 说明重要变更；避免 `docs: update`、`docs: fix`、`docs: final` 等无信息历史。
- 每个 commit 必须逻辑内聚；不得把无关的仓库清理、研究、架构决定和实现混在一起。
- Codex 不从过去任务推断 commit / push 权限；只有当前任务明确授权或用户在当前任务确认后才执行。除非用户明确要求且已审查后果，否则不得 force-push。

### Safety 与提交前检查

- `application-local.yml` 及任何真实 key、token、日志、构建产物和本机绝对路径不得提交、打印或导出。
- 保留用户已有变更；不得为清理 working tree 而使用 destructive reset、checkout、rebase、branch deletion 或 history rewriting。
- Commit 前检查 `git status --short`、`git diff` 和 `git diff --check`；commit 只能包含当前任务授权的文件。
