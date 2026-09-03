# 01 · AgentScope Java 2.0 vs Spring AI 2.0 选型分析

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-03 |
| 决策 | ✅ **选 AgentScope Java 2.0**；Spring Boot 仅作 Web 壳，**不引入 Spring AI / Spring AI Alibaba** |
| 状态 | 结论已定；依赖 **Phase 1 流式实测回执**（见 §7 风险与对策） |
| 关联 | 实证证据见 [02-Jarvis-实证分析](./02-实证-Jarvis-SpringAI-vs-AgentScope.md)；预选出处《需求说明书 v1.2》§4/§14.1 |

---

## 1. 评估背景

- 目标：为 L.E.X.I.N.G.T.O.N.（单用户陪伴型 AI，语音为主 + 文本兜底，**记忆是灵魂**）确定后端 Agent 底座。
- 候选：AgentScope Java 2.0（新事物，学习价值高） vs Spring AI 2.0（企业生态成熟）。
- 前提：本机 JDK 17 已够 AS 运行（AS 要求 17+）；Java 21 为文档预选，非硬约束。
- 用户定位：算法工程师 + Java agent 工程师；选型同时承担"锻炼算法 + Java agent 工程能力"的目的。

## 2. 评估对象与版本事实（2026-09-03 实测 Maven Central）

| 项 | AgentScope Java | Spring AI |
| :--- | :--- | :--- |
| 最新版本 | **2.0.2**（2026-08-09；另有 `2.0.2-subagent-bugfix` 变体） | **2.0.1**（2026-08-21） |
| GA 时间 | 2.0.0 GA 于 2026-07 | 2.0.0 GA 于 2026-06 |
| 基线要求 | JDK 17+，**无 Spring 依赖**（Quickstart = 裸 `main` + `HarnessAgent.builder()`） | Spring Boot **4.1** / Spring Framework 7 / Jackson 3（Boot 3 用不了） |
| 官方内核模型清单 | DashScope(Qwen) / OpenAI 兼容 / 更多 | **无 DashScope/Qwen**（官方清单之外需自接） |
| 官方 2.0 Boot starter | 无（HarnessAgent 是普通 POJO，自行包成 Bean/WS） | 有 |

> 注：本仓库旧 handoff 预选"锁 AS 2.0.0 规避 2.0.1 流式回归"——彼时未评估。当前 2.0.2 已发布，**版本策略改为：按 2.0.2 实测，遇流式回归再回退 2.0.0**（见 §7）。

## 3. 血缘澄清（先纠一个过时前提）

- AgentScope Java 2.0 是**无 Spring 依赖的纯 Java 框架**。
- Spring AI Alibaba（SAA）只对齐 **Spring AI 1.1.x**；SAA 官方 FAQ 称其**未来底层将采用 AgentScope-Java**。
- 因此需求说明书"经 SAA 接入 AgentScope"的表述**已过时**——两套体系当前并不这样衔接；本项目不必引入 SAA。

## 4. 能力对照总表

| 能力 | AgentScope Java 2.0 | Spring AI 2.0 | 判定 |
| :--- | :--- | :--- | :--- |
| Workspace / 人格文件（AGENTS.md/MEMORY.md/Skills） | ✅ 核心抽象 + 抽象文件系统（本地/MySQL/Redis/OSS） | ❌ 无（靠自研拼装） | AS 胜 |
| 类型化流式事件 | ✅ 31 种（agent 生命周期/工具/权限/计划…） | ❌ 无 | AS 胜 |
| AgentStateStore（会话/任务状态） | ✅ 内存/Json/MySQL/Redis/PG，滚动发布可恢复 | ❌ 无 | AS 胜 |
| 记忆 | ✅ 双层长期记忆 + 自动压缩策略族 + Memory Search/Get/Session Search | ❌ 仅 ChatMemory SPI（压缩/摘要自研） | AS 胜 |
| 权限 / HITL | ✅ 三态闸门（allow/deny/ask）+ 可编程 | ❌ 无（自研） | AS 胜 |
| 沙箱（不可信工具代码） | ✅ 本地/Docker/K8s/云 + 执行守卫 | ❌ 无 | AS 胜 |
| 子 agent 编排 | ✅ Fork/Spawn、同步+异步、Task List、远程子 agent | ❌ 无（自研） | AS 胜 |
| Channel（IM） | ✅ 钉钉/飞书/企微 | ❌ 无 | AS 胜 |
| async + scheduled wakeup（主动推送） | ✅ 原生 | ❌ 无 | AS 胜 |
| 多租户隔离 | ✅ session/user/agent/org 四维，RuntimeContext 贯穿 | ❌ 无 | AS 胜 |
| 分布式/观测 | ✅ OpenTelemetry 埋点、无状态水平扩展 | ⚠️ 微服务底座强，但非 agent 语义 | 平 |
| RAG / 向量生态 | ⚠️ 起步（mem0 等集成） | ✅ Embedding/向量库/ETL 管线成熟 | Spring AI 胜 |
| 社区规模 / 资料 | ⚠️ 新（2026-07 GA，中文文档完善） | ✅ 大厂背书、量大 | Spring AI 胜 |
| Agent 运行时（循环/编排/MCP） | ✅ ReAct 内核 + Harness 全套 | ⚠️ **工具循环/MCP 之外无 agent 运行时**——选它 = 自研整个 Harness | AS 胜 |

## 5. 决定性差异

1. **定位层级不同**：Spring AI 是"模型接入 + 工具调用"的**模型层**；AgentScope Java 2.0 是 **ReAct 内核之上再叠 Harness 工程层**的**完整 agent 底座**。本项目要的 workspace/人格、记忆沉淀、权限、事件流、子 agent，Spring AI 全都不提供，需要像公司 Jarvis 那样**自研 ~1 万行**（见 02 号实证文档）。
2. **记忆定位契合**：本项目记忆 M1–M3 自研（Redis/PG/pgvector）是"记忆是灵魂、不外包"的既定决策——**该决策不受选型影响**；AS 的 Memory 仅作桥接，正好保留 M2/M3 的算法锻炼点。
3. **白送底座**：AS 额外提供 workspace、事件流、权限、沙箱、子 agent、Channel、scheduled wakeup 等 Jarvis 手搓过的能力，降低首版工程量。

## 6. 对 L.E.X.I.N.G.T.O.N. 决策的影响

- ✅ 后端底座：**AgentScope Java 2.0**（maven 坐标 `io.agentscope:*`）。
- ✅ Web 壳：Spring Boot 仅作可选项（如需 REST/WS/静态资源托管），**不引 Spring AI / SAA**。
- ✅ 记忆：M1 Redis / M2 PG / M3 pgvector **自研掌控**；AS Memory 桥接（不变）。
- ✅ 多租户/多用户维度：本项目**单用户**，但 AS 的 RuntimeContext 以固定 `userId` + `sessionId` 贯穿 workspace/记忆，天然支持"多设备 + 多会话"与未来扩展。
- 📌 落地方式：`HarnessAgent` 是普通 POJO，由 server 模块包成 Bean + WebSocket/SSE 网关（对应需求书 §7.2 分层）。

## 7. 风险与对策

| 风险 | 对策 |
| :--- | :--- |
| AS 太年轻（2026-07 GA），2.0.1 曾有流式回归 issue | **按 2.0.2 实测**；以 Phase 1 流式验收（WS 首 token p95<2s）为准，不过则回退 2.0.0 |
| 无官方 2.0 Spring Boot starter | HarnessAgent 为普通 POJO，自行包 Bean/WS（server 模块职责，工程量可控） |
| 中文/社区资料少于 Spring AI | 官方中文文档 + GitHub News 能力清单足够起步；本项目单用户、单机部署，踩坑面小 |
| RAG 生态弱于 Spring AI | 本项目知识类功能非 P0；向量检索可走自研 pgvector（M3），不依赖框架 RAG |

## 8. 结论

**选 AgentScope Java 2.0**。它把"agent 运行时 + Harness 工程层"作为第一公民，与项目"人格/记忆/陪伴"的产品重心契合；Spring AI 阵营仅在 RAG/社区规模占优，且其 agent 运行时缺口已被公司 Jarvis 的 ~1 万行自研代码实证（见 02）。以 2.0.2 实测启动 Phase 1，保留回退 2.0.0 的版本策略。

---
*本文档为决策记录；与《需求说明书 v1.2》§4/§14.1 的预选冲突处以本文档为准，需求书相关小节待回写。*
