# OpenAlice 文档地图

## 当前状态

项目处于 Repository Re-initialization。当前没有已建立的产品或架构权威；请先阅读：

1. [AGENTS.md](../AGENTS.md)
2. [handoff.md](../handoff.md)
3. [活动计划](plans/active/repository-reinitialization.md)
4. [研究来源政策](research/source-policy.md)

## 稳定知识（待建立）

| 目录 | 作用 | 当前状态 |
| :-- | :-- | :-- |
| `docs/product/` | 当前产品定义、验收和用户可观察契约 | 待 Phase 3 建立 |
| `docs/architecture/` | 当前架构 spine、边界和已接受决策 | 待 Phase 4 建立 |
| `docs/plans/active/` | 当前执行计划 | 已建立 |
| `docs/plans/completed/` | 已完成计划 | 暂无 |
| `docs/research/` | 有日期、可追溯、非权威的研究 | 已建立 |
| `docs/quality/` | 测试、evaluation、质量门禁 | 待工程基础阶段建立 |

## Reference 与历史

`temp/product/` 和 `temp/engineering/` 保存本次重初始化前的材料，仅用于恢复想法、发现遗漏和追溯决策。它们不能直接决定新产品、包结构、数据库模型、依赖或框架选择。

当前 `src/` 也是 legacy reference，但不搬入 `temp/`；它暂时保留用于行为观察和可恢复的基线验证。完成产品与架构重初始化后，按活动计划决定清理。
