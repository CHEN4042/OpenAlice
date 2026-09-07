# docs/index.md · OpenAlice 文档索引

> 目的：让协作者先读索引、再按需打开对应文件；不要全量通读所有文档。
> 根目录保留：`README.md`（项目门面）、`AGENTS.md`（Codex 入口指引）、`handoff.md`（会话交接）。

## 文档清单

### 根目录

| 文档 | 概括 | 何时读 |
| :-- | :-- | :-- |
| [handoff.md](../handoff.md) | 会话交接：当前进度 / 红线 / 下一步 | 每次开工必读 |
| [README.md](../README.md) | 项目门面：定位、架构、构建运行 | 首次了解项目时 |
| [AGENTS.md](../AGENTS.md) | 仓库协作与工程规则 | 开发前必读 |

### docs/ 归档

| 文档 | 概括 | 何时读 |
| :-- | :-- | :-- |
| [项目需求说明书 v1.2](./项目需求说明书%20v1.2.md) | 核心规格 v1.5：命名体系 / P1–P4 阶段坐标 / 记忆 / 里程碑；P1 底座五项决策（砍 Redis、真实双 provider、/chat SSE 流式、单用户 + persona、Phase 坐标）已拍板 | 动需求、命名、里程碑前必读；架构以 ADR 06 / 07 为准 |
| [CONTEXT.md](./CONTEXT.md) | 共享语言与术语速查 | 术语、编号拿不准时随手查 |
| [记忆架构设计 v0.4](./记忆架构设计%20v0.4.md) | 记忆系统总体设计：M1–M3 / A1 会话归档 / 写入读取管线 / Jarvis 对照；v0.4 起 M1 去 Redis 化（PG `session_message` + 进程内 AgentState），同步需求书 v1.5 | 动记忆、画像、长期记忆前必读 |
| [decisions/](./decisions/) | 技术决策记录（ADR） | 技术选型 / 架构结论以它为准 |

### decisions/ · 决策记录

| 编号 | 文档 | 概括 | 状态 |
| :-- | :-- | :-- | :-- |
| 01 | [AgentScope Java 2.0 vs Spring AI 2.0 选型分析](./decisions/01-agentscope-vs-springai.md) | 框架级选型：AgentScope Java 2.0 | ✅ 已定 |
| 02 | [MelonPaw 参考评估](./decisions/02-melonpaw-reference.md) | AS 2.0 工程范式实证 | ✅ 已评估 |
| 03 | [Skylark 参考评估](./decisions/03-skylark-voice-reference.md) | Java 语音链路组件地图 | ✅ 已评估 |
| 04 | [OpenAlice 更名决议](./decisions/04-openalice-renaming.md) | OpenLexington → OpenAlice | ✅ 已定 |
| 05 | [OpenAlice 架构蓝图](./decisions/05-architecture-naming-evolution.md) | 讨论稿 v4：模块边界、开发规范、测试策略、扩展预留 | 🟡 已被 ADR 06 修订顶层布局 |
| 06 | [Phase 1 根目录四模块布局](./decisions/06-phase1-root-module-layout.md) | 当前架构决议：取消 `service/`，四个 Maven 模块直接放根目录，Phase 1 开始编码 | ✅ 当前有效（§3 范围由 ADR 07 扩展） |
| 07 | [P1 底座技术决策](./decisions/07-p1-base-decisions.md) | v1.5 五项拍板：砍 Redis / 真实双 provider / /chat SSE / 单用户 + persona / Phase 坐标 P1–P4；修订 ADR 01 M1-Redis 前提、ADR 06 Java 与 P1 范围 | ✅ 当前有效 |

## 建议阅读顺序

1. `handoff.md`
2. 本索引
3. ADR 06 / ADR 07
4. ADR 05 中仍有效的开发规范与测试策略
5. 按任务需要选读其他文档

## 约定

- 正文文档统一放 `docs/`；决策在 `decisions/` 内按 `NN-主题.md` 追加。
- 只追加、不改写历史结论；结论被推翻时在新 ADR 中显式标注。
- 参考资料只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。

## 整理记录

- 2026-09-07：需求书升 **v1.5**（P1 底座五项决策：砍 Redis / 真实双 provider / /chat SSE 流式 / 单用户 + persona 初始化 / Phase 坐标 P1–P4）；记忆架构设计升 **v0.4**（M1 去 Redis 化，PG `session_message` + 进程内 AgentState）；新增 **ADR 07**（P1 底座技术决策）；CONTEXT 同步阶段坐标。
- 2026-09-07：新增《记忆架构设计 v0.1》（草案）：沉淀记忆分层、写入管线与 Jarvis 对照，待用户确认 §6 治理修订项后同步需求书。
- 2026-09-07：记忆架构设计升 v0.2：明确 PG 表为主存储、M1 = AgentScope `AgentStateStore` Redis 适配、补充 A1 会话归档表、Profile 检索常驻加权；写入管线按用户指示直接走真实 LLM。
- 2026-09-07：记忆架构设计升 **v0.3（定稿）**：存储选型（PG 表为主、文件仅调试导出）与 §6 治理修订（bi-temporal + 遗忘曲线）经用户拍板，同步《项目需求说明书》v1.4（§8.2 治理细则 / §8.3 存储选型与归档）。

- 2026-09-04：`docs/README.md` 更名本文件，确立“先读索引、按需选读”约定。
- 2026-09-04：需求 / 架构文档归档至 `docs/`，决策记录移入 `docs/decisions/`。
- 2026-09-06：项目更名 OpenLexington → OpenAlice；需求书升 v1.3；新增 ADR 04。
- 2026-09-06：旧四模块代码骨架建立后又按用户要求回滚；新增 ADR 05 讨论新架构。
- 2026-09-06：删除旧《系统架构图 v2.0》，架构讨论统一收敛到 ADR 05。
- 2026-09-06：ADR 05 升级为讨论稿 v4，补充规范、测试、语音 / learning / OpenHanako 式扩展预留。
- 2026-09-06：新增 ADR 06：用户确认取消 `service/` 聚合层，四个 Maven 模块直接放在仓库根目录；Phase 1 编码启动。
