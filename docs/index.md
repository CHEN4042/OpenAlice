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
| [项目需求说明书 v1.2](./项目需求说明书%20v1.2.md) | 核心规格 v1.5：命名体系 / P1–P4 阶段坐标 / 记忆 / 里程碑；P1 底座五项决策（砍 Redis、真实双 provider、/chat SSE 流式、单用户 + persona、Phase 坐标）已拍板 | 动需求、命名、里程碑前必读；架构以 ADR 07 / 08 为准 |
| [CONTEXT.md](./CONTEXT.md) | 共享语言与术语速查 | 术语、编号拿不准时随手查 |
| [记忆架构设计 v0.4](./记忆架构设计%20v0.4.md) | 记忆系统总体设计：M1–M3 / A1 会话归档 / 写入读取管线 / Jarvis 对照；v0.4 起 M1 去 Redis 化（PG `session_message` + 进程内 AgentState），同步需求书 v1.5 | 动记忆、画像、长期记忆前必读 |
| [代码学习导览 v0.1](./代码学习导览%20v0.1.md) | 代码学习地图：模块全景 + `POST /chat` 主线链路 + 真实双 provider 专题 + 学习追踪表；**随代码维护** | 想读懂代码 / 边做边学时必读 |
| [decisions/](./decisions/) | 技术决策记录（ADR） | 技术选型 / 架构结论以它为准 |

### decisions/ · 决策记录

| 编号 | 文档 | 概括 | 状态 |
| :-- | :-- | :-- | :-- |
| 01 | [AgentScope Java 2.0 vs Spring AI 2.0 选型分析](./decisions/01-agentscope-vs-springai.md) | 框架级选型：AgentScope Java 2.0 | ✅ 已定 |
| 02 | [MelonPaw 参考评估](./decisions/02-melonpaw-reference.md) | AS 2.0 工程范式实证 | ✅ 已评估 |
| 03 | [Skylark 参考评估](./decisions/03-skylark-voice-reference.md) | Java 语音链路组件地图 | ✅ 已评估 |
| 04 | [OpenAlice 更名决议](./decisions/04-openalice-renaming.md) | OpenLexington → OpenAlice | ✅ 已定 |
| 05 | [OpenAlice 架构蓝图](./decisions/05-architecture-naming-evolution.md) | 讨论稿 v4：模块边界、开发规范、测试策略、扩展预留 | 🟡 顶层布局经 ADR 06 → 再经 ADR 08 修订 |
| 06 | [Phase 1 根目录四模块布局](./decisions/06-phase1-root-module-layout.md) | 取消 `service/`，四个 Maven 模块直接放根目录（**顶层布局已被 ADR 08 修订**） | ✅ 已被 ADR 08 修订顶层布局 |
| 08 | [单模块收敛决议](./decisions/08-single-module-convergence.md) | 四模块收敛为单模块 + service 编排；根包与目录细节已被 ADR 09 修订 | 🟡 历史有效 |
| 10 | [业务包自治与演进触发规则](./decisions/10-package-layout-evolution-rules.md) | **当前包结构决议**：业务包（chat/service·store）+ 内核（agent）+ 共享 model；包内按类型整理；共享才上提；演进触发规则 | ✅ 当前有效 |
| 11 | [LLM 配置落 yml + local profile](./decisions/11-llm-configuration-local-profile.md) | LLM 配置默认值进 application.yml；本机真实 key 走 gitignored `application-local.yml`；`config.OpenAliceSettings` 绑定 | ✅ 当前有效 |
| 12 | [UserId/SessionId 降为 String 字段](./decisions/12-user-session-id-as-string.md) | 删除独立值对象，id 直接 String 字段；已被 ADR 13 进一步收敛（归属下沉 `StoredMessage`） | 🟡 已被 ADR 13 修订消息层细节 |
| 13 | [ChatMessage 职责拆分与冗余清理](./decisions/13-chat-message-responsibility-split.md) | **当前消息模型决议**：`ChatMessage` 只留 role/content；归属/时间戳下沉 `chat.store.StoredMessage`；删 Turn 状态机 / blocking 便利层 / 重复 DTO | ✅ 当前有效 |
| 09 | [P1.5 语义重构与包结构整理](./decisions/09-p15-semantic-and-package-structure.md) | `com.openalice` 根包、ConversationStore、Turn、AgentRequest/AgentEvent、ContextAssembler、SessionCoordinator、单用户 API | 🟡 语义仍有效；顶层布局已被 ADR 10 修订 |
| 07 | [P1 底座技术决策](./decisions/07-p1-base-decisions.md) | v1.5 五项拍板：砍 Redis / 真实双 provider / /chat SSE / 单用户 + persona / Phase 坐标 P1–P4；修订 ADR 01 M1-Redis 前提、ADR 06 Java 与 P1 范围 | ✅ 当前有效 |

## 建议阅读顺序

1. `handoff.md`
2. 本索引
3. ADR 10（当前包结构）/ ADR 13（当前消息模型）/ ADR 11（当前配置：yml + local profile）/ ADR 09（P1.5 语义）/ ADR 07（P1 底座决策）
4. ADR 05 中仍有效的开发规范与测试策略
5. 按任务需要选读其他文档
6. 想读懂代码 / 边做边学：读《代码学习导览 v0.1》并按其 §8 更新学习追踪表

## 约定

- 正文文档统一放 `docs/`；决策在 `decisions/` 内按 `NN-主题.md` 追加。
- 只追加、不改写历史结论；结论被推翻时在新 ADR 中显式标注。
- 参考资料只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。

## 整理记录

- 2026-09-08：新增 **ADR 13**——`ChatMessage` 收敛为纯 LLM 消息（role/content）；新增 `chat.store.StoredMessage` 承载 id/归属/时间戳；`ConversationStore.history` 按 sessionId 收口；删除 `ConversationTurn`/`TurnStatus`（进程内零消费状态机，ADR 09 §4.2 不提前加死接口）、`ChatService.chat()`/`AgentRuntime.chat()` blocking 与 `ChatResponse`/`ChatResult`、`ChatStreamEvent.sessionId` 冗余字段；controller 双 if-else 收敛为 switch；`DEFAULT_USER_ID` 下沉 `ChatService` 私有。
- 2026-09-08：新增 **ADR 12**——删除 `UserId` / `SessionId` 值对象，`ChatMessage` / `ConversationTurn` 直接 `String userId / sessionId` 字段（Jarvis 直观风格），`UserId.DEFAULT` → `ChatMessage.DEFAULT_USER_ID`，校验内聚构造器；测试与 AGENTS / README / CONTEXT / 学习导览 / handoff 同步；22 tests 全绿（Java 21）。
- 2026-09-08：新增 **ADR 11**——LLM 配置默认值移入 `application.yml`，本机真实 key 走 gitignored `application-local.yml`（`local` profile）；`AgentRuntimeProperties` 收敛为纯配置容器 + 新增 `config.OpenAliceSettings` 绑定；README / CONTEXT / handoff 同步；22 tests 全绿（Java 21）。
- 2026-09-08：新增 **ADR 10**——业务包自治 + 包内按类型整理 + 演进触发规则；`repository/` → `chat/store/`、`service/` → `chat/service/`；AGENTS.md / README / CONTEXT / 学习导览 / handoff 同步；22 tests 全绿（Java 21）。
- 2026-09-07：新增 **ADR 09**——根包改为 `com.openalice`，`port + memory` 收敛为 `repository`，新增 Turn / ConversationStore / AgentRequest / AgentEvent / ContextAssembler / SessionCoordinator，并改为单用户 API。
- 2026-09-07：新增 **ADR 08**——四 Maven 模块收敛为单模块（根 `pom.xml` 即应用），顶层包分层 `model/port/memory/agent/service/controller/dto/config`；新增 `ChatService` 编排层与 `OpenAliceConfiguration` 组合根；删除 0 引用占位；AGENTS.md / README / handoff / 学习导览同步。
- 2026-09-07：命名与结构微调——新增顶层 `enums/` 包（MessageRole / LlmProvider 集中，均零依赖）；`ChatApiController → ChatController`、`ChatReply → ChatResponse`（Request/Response 配对）；AGENTS / README / CONTEXT / handoff / 学习导览同步。
- 2026-09-07：新增《代码学习导览 v0.1》（代码学习地图：模块全景 / `POST /chat` 主线链路 / 双 provider 专题 / 学习追踪表；随代码维护，见其 §10）。

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
