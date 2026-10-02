# Repository Re-initialization

> 状态：ACTIVE
> 范围：建立新仓库基础；不包含新的业务实现。

## 目标

在新产品实现开始前，把 OpenAlice 重建为一个适合人和 Coding Agent 长期共同维护的清晰基础。旧项目可用于参考，但不定义新产品、架构或实现。

## 阶段

### Phase 1 — Repository foundation

- 建立新的 `README.md`、`AGENTS.md`、`handoff.md` 和 `docs/index.md`。
- 建立 `docs/product/`、`docs/architecture/`、`docs/plans/`、`docs/research/`、`docs/quality/` 与 `evals/`。
- 建立 `docs/research/source-policy.md`。
- 保持文档入口短、职责单一，并明确当前没有产品/架构 authority。

### Phase 2 — Legacy reference cleanup

- 把旧产品材料放入 `temp/product/`：旧需求、产品想法、人格/对话规则和产品级记忆要求。
- 把旧工程材料放入 `temp/engineering/`：旧 architecture、ADR、工程研究、记忆架构、代码导览和技术上下文。
- `temp/` 只保存参考文档与材料，不保存旧业务实现。
- foundation 提交 `bd4618f` 推送后，从 active repository 删除 legacy `src/`、`web/`、`pom.xml` 和 `compose.yaml`；旧实现继续通过 Git 历史恢复。
- cleanup 完成后直接进入 Product Re-initialization；在产品和架构重新初始化完成前不创建替代业务实现。

### Phase 3 — Product re-initialization

- 基于当前目标重新进行产品澄清。
- 可参考 `temp/product/`，但必须重新判断并识别已过时的结论。
- 将当前产品定义建立在 `docs/product/`，结构按实际知识量决定，不机械复制旧 SPEC 结构。

### Phase 4 — Architecture re-initialization

> **状态：COMPLETED（2026-10-02）** · 当前权威：[Architecture Spine](../../architecture/ARCHITECTURE-SPINE.md)

- 以新的产品定义为输入重新研究架构。
- 遵守 `docs/research/source-policy.md`。
- 可参考 `temp/engineering/`，但不得从旧 package、依赖、数据库模型或 framework choice 推导新架构。
- 将新架构建立在 `docs/architecture/`，并明确所有权、边界、依赖方向、验证方式和 deferred 项。

### Phase 5 — Engineering foundation

> **状态：NOT STARTED** · 首个前置步骤是针对当前 Architecture Authority 运行 bounded AgentScope Technical Spike。

只有产品与架构决定形成后，才按实际需要建立 Maven wrapper、source/test layout、CI、自动化测试、AI evaluation、observability、dependency/security checks 和 local development infrastructure。

### Phase 6 — Remove temporary material

当产品和架构不再依赖旧材料后：

- 验证有价值的结论已经重新判断或写入新的权威文档；
- 删除 `temp/`；
- 删除不再有用的生成物或工具特定结构；
- 更新所有仓库导航和交接路径。

## 变更规则

- 旧材料是 reference，不是 authority。
- 重要知识只保留一个当前版本，避免复制漂移。
- 不在产品和架构重初始化完成前开始新的业务实现。
- 不因为旧 ADR 或旧代码存在就继承技术决定。
- 研究提供证据，项目文档做最终判断。

## 验证

结构变更至少验证：

- 预期文件和目录存在；
- 仓库内部链接可解析；
- 被移动内容仍可从 Git 恢复；
- 没有引入 secret、个人信息、本机路径、日志或构建产物；
- commit 前检查 `git status`。

## 完成条件

重初始化完成必须同时满足：

- 根入口文档和文档地图建立；
- `docs/product/` 存在当前产品定义；
- `docs/architecture/` 存在当前架构；
- `temp/` 不再被依赖并已删除；
- obsolete legacy implementation 不再位于 active repository；
- 仓库可以进入第一轮新实现。

完成后将本计划移动到 `docs/plans/completed/repository-reinitialization.md`，并把 `handoff.md` 更新为下一项 active task。
