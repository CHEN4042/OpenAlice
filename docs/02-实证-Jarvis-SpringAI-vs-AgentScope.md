# 02 · Jarvis 实证：Spring AI 自研 vs AgentScope Java 2.0

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-03 |
| 结论 | 某公司内部项目（下文称 Jarvis）用 Spring AI 自研了一整套 Harness（agent 运行时工程层），约 **9.4k 行 / 61 文件**；其逐项能力 ≈ AgentScope Java 2.0 内置组件 |
| 目的 | 为 [01-选型分析](./01-选型-AgentScope-vs-SpringAI-2.0.md) 提供**代码级实证**，回答"Spring AI 到底缺什么、自研要花多少" |
| ⚠️ 保密 | Jarvis 为**公司内部项目**；本仓库为 **public**，本文档已按"无内部类名 / 包路径 / 表名 / 组织标识 / 本机路径"标准脱敏。以下仅保留**架构级结论** |

---

## 1. Jarvis 技术画像（实证对象）

| 项 | 值 |
| :--- | :--- |
| 定位 | 公司内部、自研从 0 到 1 的对话式智能体服务（ReAct 模式 AI 助手） |
| 技术栈 | Java 17 · Spring Boot 3.5.x · **Spring AI 1.1.0-M4**（里程碑版）· Spring AI Alibaba 1.1.0.0-M5 |
| 传输 | WebFlux + SSE 流式 + 对话中途停止 |
| 存储 | SQLite（本地库文件）+ MyBatis；Ehcache |
| 规模 | 412 个 Java 文件 / **42,177 行**（仅源码） |
| agent 运行时包 | `agents/`：**9,392 行 / 61 文件** |

自研 harness 层分布：工具 4,925 行 · 技能 1,229 · 记忆 863 · 子 agent 856 · 提示词装配 650 · token 追踪 276 · advisor 161。另公共层含自研 SSE 事件协议等 5,595 行。

## 2. Jarvis 架构解剖（它把 Spring AI 用在哪、自研到哪）

| 层 | Jarvis 的做法 | Spring AI 提供 | 自研位置（jarvis-server 内相对包级） |
| :--- | :--- | :--- | :--- |
| Agent 循环 | `ChatClient.prompt().system().user().advisors(记忆).tools(...).stream()` | 自动"思考→工具→观察"循环（未手写 ReAct 状态机） | `agents/`：多模型选择、SSE 转发、响应式订阅中断 |
| 上下文/记忆 | token 超限 → 压缩 + LLM 摘要 | 仅 `ChatMemory/ChatMemoryRepository` SPI | `agents/memory/`：摘要记忆、SQLite/TiDB 仓库、token 计数（压缩策略/prompt 全自研） |
| 人格/系统提示词 | SOUL/AGENTS/PROFILE/Skills/运行时 动态拼装，DB 驱动 + 兜底 | 无 | `agents/prompt/`：系统提示词装配器 + 人格市场（按用户/场景隔离） |
| 技能系统 | 三级渐进披露：元数据→SKILL.md 正文→捆绑资源；classpath+用户目录双层；热刷新 | 无 | `agents/skill/`：技能注册中心 + 读/刷新技能工具 |
| 子智能体 | 静态 + 动态子 agent；**本质 = 每子 agent 一个 ChatClient**；主 agent 经工具触发；上下文手动拷贝 | 无 | `agents/subagent/`：工厂/调用器/注册表/动态创建 |
| 安全确认(HITL) | 规则表 + 白名单/正则 → 确认事件 → 阻塞线程等前端回调（超时默认） | Advisor 仅钩子 | 服务层二次确认服务 + `agents/advisors/check/` 请求前过滤链 |
| 事件/SSE | 自研 ~18 种字符串事件（data/think/log/todos/confirm/token/file/completed…） | 无 | 公共层 SSE 事件发送工具 |
| Token 追踪 | thinking / tool-planning / system 分类，按工具类别归因 | 仅原始 usage 元数据 | `agents/token/`：token 归因追踪器 |
| 多租户 | 用户+场景标识 **手工贯穿** DB 过滤 / 技能 / workspace 文件路径 | 无 | 请求上下文对象 + workspace 路径工具 |
| 定时/异步 | CRON + 动态注册；asyncTask 工具 | 无 | 调度模块 + 异步任务工具 |
| ⚠️ 命令/Python 执行 | **直接本机执行**（无沙箱，靠确认规则兜底） | 无 | 命令行工具 + Python 执行工具 |

**反直觉点**：Jarvis 手写的不是 ReAct 循环（Spring AI 包了），而是 **Harness 层**——而这正是 AgentScope 2.0 的定位（"ReAct 内核之上加 Harness 工程层"）。两者在**同一层**竞争。

## 3. 逐项差距：Jarvis 自研 vs AgentScope Java 2.0 内置

| Jarvis 自研能力 | AgentScope Java 2.0 对应 | 差距判断 |
| :--- | :--- | :--- |
| 单层"超限→摘要"压缩 | 上下文压缩**策略族**（截断落盘/入参截断/消息压缩/保留最近）+ 可定制压缩提示词 | AS 策略更全 |
| 长期记忆工具组（每日/长期/画像/心跳/搜索/任务） | **双层长期记忆**（每日流水账 + 后台蒸馏 MEMORY.md）+ Memory Search/Get/Session Search 工具 | Jarvis 工具组 ≈ AS 内置 1:1 复刻 |
| 按会话分目录 + 文件工具 | **Workspace 抽象** + 抽象文件系统（本地/MySQL/Redis/OSS）+ AGENTS.md/MEMORY.md 约定 | Jarvis 是字符串拼路径；AS 是框架级概念 |
| 技能三级渐进披露 | **Skills 体系** + 注册中心对接 + 技能沙箱内闭环执行 | 设计同源（均受 Claude/nanobot skills 启发）；AS 做成框架件 |
| 子 agent（同步、手动拷上下文） | **Fork/Spawn + 同步/异步子 agent + 完成通知主 agent + 远程子 agent + Task List + 事件透传/权限继承** | **差距最大之一**：异步回调、任务状态追踪、权限继承在 Jarvis 需继续自研 |
| 二次确认（全局规则表） | **Permission 三态闸门**（allow/deny/ask HITL）+ 可编程 | Jarvis ≈ AS 子集，且非 per-tool 可编程 |
| ~18 种字符串事件 | **31 种类型化 streamEvents** | Jarvis 靠字符串协议 + 前端约定；AS 有结构化事件模型 |
| ⚠️ 命令/Python 裸执行 | **Sandbox**（本地/Docker/K8s/云）+ 执行守卫 + 沙箱状态跨进程恢复 | **AS 最大安全增量**：公司内网靠统一认证+确认规则兜底，个人/公网场景是硬伤 |
| 单机库会话表 | **AgentStateStore**（内存/Json/MySQL/Redis/PG）+ 滚动发布状态恢复 | Jarvis 多实例部署需自行解决文件锁/状态同步 |
| 用户+场景手工隔离 | **org/user/agent/session 四维隔离**，键贯穿 workspace/KV/沙箱 | Jarvis 是"约定式"隔离，漏一处就串数据 |
| CRON 动态注册 | 内置 async + **scheduled wakeup**（→ 主动推送） | Jarvis 自研 ≈ AS 原生 |
| 自研日志 + 前端 token 面板 | 内置 **OpenTelemetry 埋点** | AS 观测更标准 |

**量化**：`agents/` 9,392 行 ≈ 在 AgentScope 里用"依赖 + 配置"换掉的部分；再加自研 SSE 协议。**保守估计 harness 自研投入 1 万行以上**，不含调试维护。

## 4. 诚实边界（AgentScope 也给不了你的）

Jarvis 有几块优势**不在框架**，AS 也不提供，迁移时照样自研：

1. **管理面产品功能**：场景/技能市场、可见性管理、审核流、知识库训练管理（doc/qa/ddl）、报表、宠物陪伴规则引擎——业务创新层，与选型正交。
2. **RAG/向量生态**：Spring AI 的 Embedding/向量库/ETL 管线成熟度仍高于 AS 2.0——**Spring AI 阵营唯一硬优势**。
3. **企业中间件**：统一认证、配置中心、公司自研安全框架——与 agent 框架无关，套壳即可。
4. **Spring 全家桶一致性**：Jarvis 深度绑定 Boot/WebFlux/MyBatis；AS 无 Spring 依赖，需自行把 HarnessAgent 包成 Bean/WS。

## 5. 建议（分三个场景）

- **OpenLexington（新项目，选 AS）**：本实证坐实选型——原本计划在 Lexington 手写的"记忆三层、workspace、事件、权限"正是 Jarvis 手搓过的 ~9k 行。用 AS 后精力可全部投向**人格/记忆算法/语音链路/Web UI**（算法 + Java 锻炼点）。
- **公司 Jarvis（存量，不推翻）**：业务 + 管理面绑定深，建议把 AgentScope 当**下一代底座技术预研**，新场景试点；若渐进改造，收益最大三块：① 沙箱（命令/Python 裸执行为最大隐患）② 会话状态存储化（多实例/滚动发布）③ 事件协议标准化。
- **个人成长**：Jarvis 的工具分类/token 追踪/二次确认经验在 AS 中 **1:1 有对应概念**；练 Lexington 时可用 Jarvis 能力清单当"验收清单"，逐个验证 AS 如何用配置替代手写代码。

## 6. 说明

- 上述行数/架构为 2026-09-03 对本机 Jarvis 仓库 `jarvis-server` 模块的**静态统计与阅读所得**，结论在脱敏后仍可复核（模块结构不变）。
- 本文档为**架构级决策记录**，不含任何公司内部代码标识；完整代码级对照仅供本机私有环境使用，不落入本 public 仓库。

---
*本仓库为 public；涉及公司内部项目的内容一律只保留架构级结论。*
