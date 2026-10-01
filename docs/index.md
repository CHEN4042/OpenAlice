# OpenAlice 文档地图

## 当前状态

项目处于 Repository Re-initialization。产品权威已经建立，架构权威尚未建立；请先阅读：

1. [AGENTS.md](../AGENTS.md)
2. [handoff.md](../handoff.md)
3. [活动计划](plans/active/repository-reinitialization.md)
4. [研究来源政策](research/source-policy.md)

## 稳定知识（待建立）

| 目录 | 作用 | 当前状态 |
| :-- | :-- | :-- |
| `docs/product/` | 当前产品定义、验收和用户可观察契约 | [OpenAlice-产品规格说明.md](product/OpenAlice-产品规格说明.md)（当前产品权威） |
| `docs/architecture/` | 当前架构 spine、边界和已接受决策 | 待 synthesis review 后建立 |
| `docs/plans/active/` | 当前执行计划 | 已建立 |
| `docs/plans/completed/` | 已完成计划 | 暂无 |
| `docs/research/` | 有日期、可追溯、非权威的研究 | 已建立 |
| `docs/quality/` | 测试、evaluation、质量门禁 | 待工程基础阶段建立 |

## 当前研究与协作

以下均为非权威研究输入，不构成已接受的架构决策：

- **待 review：** [2026-10-01 Architecture Synthesis Draft](research/2026-10-01-Architecture-Synthesis-Draft.md)
- [2026-10-01 架构讨论阶段快照](research/2026-10-01-架构讨论阶段快照.md)
- [2026-10-01 Agent 架构关键问题调研](research/2026-10-01-Agent架构关键问题调研.md)
- [2026-10-01 Background / Task / Proactivity 第二轮调研](research/2026-10-01-Background-Task-Proactivity-第二轮调研.md)
- [BMAD 与 Web ↔ Codex 工作流](bmad-workflow.md)

## Reference 与历史

`temp/product/` 和 `temp/engineering/` 只保存本次重初始化前的参考文档与材料，用于恢复想法、发现遗漏和追溯决策。它们不包含 legacy implementation，也不能直接决定新产品、包结构、数据库模型、依赖或框架选择。

legacy implementation、旧 `web/`、`pom.xml` 和 `compose.yaml` 已从 active repository 移除。需要查看旧实现时，通过 Git 历史或已推送的 `bd4618f` 恢复；产品与架构重新初始化完成前，不创建替代业务实现。
