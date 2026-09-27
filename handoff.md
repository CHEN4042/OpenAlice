# OpenAlice Handoff

> 轻量交接文件：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前快照

| 项目 | 内容 |
| :-- | :-- |
| 日期 | 2026-09-27 |
| 阶段 | **Repository Re-initialization · Phase 1/2** |
| 分支 | `main` |
| Git 基线 | 开始本轮前为 `38dbe0c`，与 `origin/main` 对齐；本轮尚未 commit/push |
| 产品权威 | 尚未重新建立；旧 SPEC 已移入 `temp/product/` 作为 reference |
| 架构权威 | 尚未建立；旧 spine 已移入 `temp/engineering/` 作为 reference |
| 业务代码 | `src/` 保留为 legacy baseline；本轮未修改 |

## 已完成

- 阅读并执行用户提供的 Repository Re-initialization 计划。
- 建立新的根级 README、AGENTS、handoff 和 docs index。
- 建立 `docs/product/`、`docs/architecture/`、`docs/plans/`、`docs/research/`、`docs/quality/` 与 `evals/` 目录。
- 增加研究来源政策 `docs/research/source-policy.md`。
- 将旧产品材料移入 `temp/product/`，将旧架构、ADR、研究和代码导览移入 `temp/engineering/`。
- 保留 Git 历史、旧业务实现和可运行基线；没有删除源代码。

## 验证证据

- 文件移动使用 Git 可追踪 rename，旧内容仍在当前工作区和 Git 历史中可恢复。
- 业务代码未改动；本轮尚未运行实现构建，因为只进行了仓库结构和文档初始化。
- 仓库内部 Markdown 链接检查：`MISSING_LINKS=0`；`git diff --check` 通过。
- 业务实现构建/测试暂不作为本轮门禁；新的工程验证命令在 Phase 5 再建立。

## 唯一下一步

执行 Phase 3：基于 `temp/product/` 的 reference 重新进行产品澄清，并把当前产品定义建立在 `docs/product/`。未完成前，不开始架构定稿或业务重写。

## 禁止事项

- 不把 `temp/` 中的旧文档直接恢复为 authority。
- 不扩展或重构当前 `src/`。
- 不删除 `temp/`，直到产品和架构重新初始化明确不再依赖它。
- 不提交或 push；等待用户 review 本轮结构变更。
