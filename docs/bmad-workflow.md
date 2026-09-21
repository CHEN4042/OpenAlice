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

1. **Forge**：`bmad-forge-idea`，把半成品 idea 压力测试到可行动或可放弃。
2. **SPEC**：`bmad-spec`，生成产品 kernel 和 companions。
3. **Architecture**：`bmad-architecture`，生成技术 invariants 与 architecture spine。
4. **Story / Build**：拆分 story 后由独立 builder 实施。
5. **Review**：独立 reviewer 检查实际 diff 与契约。
6. **Handoff**：刷新当前状态、证据和下一步。

## 当前权威规则

- 产品问题以 `_bmad-output/specs/spec-openalice/` 为准。
- 技术问题以 `_bmad-output/architecture/openalice/ARCHITECTURE-SPINE.md` 为准。
- `docs/archive/pre-rebuild-2026-09-20/` 只作历史追溯。
- 旧 `docs/decisions/` 已归档，不作为当前 ADR 体系继续追加。
