# OpenAlice Handoff

> 轻量交接文件：只记录当前状态和唯一下一步，不承载稳定产品或架构决策。

## 当前快照

| 项目 | 内容 |
| :-- | :-- |
| 日期 | 2026-09-27 |
| 阶段 | **Repository Re-initialization · Product definition established; ready for Architecture Re-initialization** |
| 分支 | `main` |
| 已推送清理基线 | `c3f5979`（legacy cleanup 完成） |
| 当前 Git 状态 | 仅包含本轮产品规格权威同步，未改业务实现 |
| 产品权威 | [OpenAlice-产品规格说明.md](docs/product/OpenAlice-产品规格说明.md)；`temp/product/` 仅作 reference |
| 架构权威 | 尚未建立；旧 spine 已移入 `temp/engineering/` 作为 reference |
| legacy implementation | 已从 active repository 移除；可从 Git 历史或 `bd4618f` 恢复 |

## 已完成

- 阅读并执行用户提供的 Repository Re-initialization 计划。
- 建立新的根级 README、AGENTS、handoff 和 docs index。
- 规划 `docs/product/`、`docs/architecture/`、`docs/plans/`、`docs/research/`、`docs/quality/` 与 `evals/` 作为未来知识和评测位置；空目录不会单独写入 Git，真实内容出现后再建立。
- 增加研究来源政策 `docs/research/source-policy.md`。
- 将旧产品材料移入 `temp/product/`，将旧架构、ADR、研究和代码导览移入 `temp/engineering/`。
- 从 active repository 删除 `src/`、`web/`、`pom.xml` 和 `compose.yaml`；旧实现继续由 Git 历史保存。
- 旧 Maven `target/` 构建缓存已移出仓库；它是 ignored 生成物，不属于提交内容。
- `temp/` 只保存参考文档与材料，没有迁入旧业务实现。
- 为避免不可恢复地删除真实 key，原本位于 legacy `src/` 下且被 gitignore 的 `application-local.yml` 已原样移到仓库根目录；内容未读取，仍不会进入 Git。
- 已确认用户新增的产品规格为当前产品权威；旧 SPEC、Forge 和其他 `temp/product/` 材料不再覆盖本文。

## 验证证据

- foundation 已以 `bd4618f` 推送，删除内容可从 Git 历史恢复。
- `src/`、`web/`、`pom.xml`、`compose.yaml` 已不存在。
- 本轮不运行 Maven/build 命令，因为 legacy Maven project 被有意移除。
- 仓库内部 Markdown 链接检查：`MISSING_LINKS=0`；`git diff --check` 通过。
- 已运行 `git status --short`，只有本轮指定文档更新和 legacy 删除；根目录 `application-local.yml` 继续被 `.gitignore` 排除。

## 唯一下一步

进入 Architecture Re-initialization：以当前 `docs/product/OpenAlice-产品规格说明.md` 为唯一产品输入，重新研究并建立 `docs/architecture/`。在架构完成前不开始业务实现。

## 禁止事项

- 不把 `temp/` 中的旧文档直接恢复为 authority。
- 不创建新的业务源码、Maven 文件、placeholder source directory 或架构实现。
- 不删除 `temp/`，直到产品和架构重新初始化明确不再依赖它。
- 后续 commit 或 push 仍需用户明确指示。
