# BMAD 与 duoagent 工作流

## 安装与换机恢复

当前使用 Codex 插件：

- `bmad-method@bmad`
- `bmad-toolbox@bmad`
- 版本：`6.13.0-next`
- 冻结 marketplace commit：`d009608292d8a2ea4df846de7dca2f0d78a9e22d`
- 依赖：`uv`

新机器可按以下方式恢复：

```bash
mkdir -p "${CODEX_HOME:-$HOME/.codex}/marketplaces"
git clone https://github.com/bmad-code-org/bmad-plugins.git "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins"
git -C "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins" checkout d009608292d8a2ea4df846de7dca2f0d78a9e22d
codex plugin marketplace add "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins"
codex plugin add bmad-method@bmad
codex plugin add bmad-toolbox@bmad
codex plugin list
```

安装后新开 Codex task，让 skill 重新加载。若项目内 `_bmad/` 不存在，运行 `bmad setup` 初始化项目脚本与配置。

## 当前流程

1. **Repository foundation**：建立根入口、文档地图、研究来源政策和轻量 handoff。
2. **Reference cleanup**：把旧产品/工程材料放入 `temp/`；legacy implementation 已从 active repository 移除，仍可通过 Git 历史恢复。
3. **Product re-initialization**：重新澄清产品，并把当前定义写入 `docs/product/`。
4. **Architecture re-initialization**：基于新产品定义研究并写入 `docs/architecture/`。
5. **Engineering foundation**：按实际需要建立 Maven、CI、测试、evaluation、observability 和安全门禁。
6. **Story / Build / Review**：只有上述基础完成后，才由独立 builder 实施并由 reviewer 验证。
7. **Handoff**：刷新当前状态、证据和唯一下一步。

## 当前权威规则

- 当前产品 authority 已建立于 `docs/product/OpenAlice-产品规格说明.md`；架构 authority 尚未建立。执行入口是 `docs/plans/active/repository-reinitialization.md`。
- 产品重新澄清后，以 `docs/product/` 为准；架构重新设计后，以 `docs/architecture/` 为准。
- `temp/product/` 和 `temp/engineering/` 只作 reference；不能直接恢复旧结论。
- `_bmad-output/` 是 BMAD 运行时的生成区，不是长期项目知识的权威入口。

## Web ↔ Codex Research Loop

Git / Codex 执行规则以 [AGENTS.md](../AGENTS.md) 为规范来源。本流程只说明 Web、Codex 与用户之间的交接：

1. 用户 + Web 澄清产品或架构问题。
2. Web 定义有边界的 research 或 implementation scope。
3. Codex 读取仓库 authority 与当前 context，执行该有边界任务。
4. Codex 记录研究证据或实现，并按要求刷新 handoff。
5. 当前文档阶段在 `main` 上形成 coherent commits；实现阶段在 User + Web 指定的 Cycle branch 上工作。Commit / push 仍须当前任务授权。
6. Codex 按当前授权 push 对应分支；实现阶段通过 Cycle Review 和单个 PR 进入 `main`。
7. Web review 仓库中的真实文件与 diff。
8. 用户 + Web 综合证据并接受或拒绝结论。
9. 只有已接受的稳定决定才进入 `docs/product/` 或 `docs/architecture/` authority。

Research note 不会自动成为 architecture authority。文档阶段的新 prompt、研究问题或模型变更都不构成新建分支的理由；只有开始真实实现后才启用由 User + Web 命名的 `openalice-YYYYMMDD` Cycle branch。
