# 02 · MelonPaw 参考评估（AgentScope 2.0 工程范式实证）

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-04 |
| 性质 | **参考评估（非选型决策）** —— 外部开源工程实证，供 Phase 1 骨架与架构设计参考 |
| 状态 | ✅ 已评估；其中 §4「记忆边界」对 01 结论挂 **待 Phase 1 重验** 标记 |
| 关联 | [01 · AgentScope Java 2.0 vs Spring AI 2.0 选型分析](./01-agentscope-vs-springai.md)（框架选型）；Skylark（语音链路）另立 [03](./03-skylark-voice-reference.md) |
| 参考对象 | [melon1010/MelonPaw](https://github.com/melon1010/MelonPaw)（Apache-2.0）—— QwenPaw 的 Java 后端复刻，个人 AI Agent 平台 |

---

## 1. 评估背景

- 目的：为 OpenLexington 找 **AgentScope Java 2.0 的真实工程级代码参考**。官方 Quickstart 是几十行最小样例，缺少产品级范式（多模块切分、工具/中间件/记忆/权限/Web 壳怎么组织）。
- 候选：MelonPaw —— 目前公开仓库中与本项目形态（单用户个人 AI 助手）最接近的 AS 2.0 完整工程。

## 2. 对象与技术栈事实（2026-09-04 源码核实）

| 项 | 事实 |
| :--- | :--- |
| AgentScope | **2.0.0-RC3**（`io.agentscope:agentscope-harness` + `agentscope-core`，Maven Central 直引） |
| Web 壳 | Spring Boot 3.3.0（**WebFlux 响应式**）+ SSE 流式推送 |
| Java | 17 |
| 模块切分 | `melon-plugin-api`（SPI）/ `melon-core`（agent·config·middleware·provider·plan·security…）/ `melon-tools` / `melon-coding-mode` / `melon-channels` / `melon-app`（Spring Boot）/ `console`（React+TS+Vite） |
| 工具 | 20+：fileio / shell / browser / agent / media / util / cron / skill |
| 中间件 | 8 个：SystemPrompt / MediaFilter / AutoContinue / CodingMode / TokenRecording / PlanGate / MemoryInjection（已标 @Deprecated）… |
| 模型供应商 | DashScope / OpenAI / Anthropic / Gemini / DeepSeek / Ollama |

## 3. 工程范式要点（Phase 1 骨架可复用）

### 3.1 HarnessAgent 完整装配（对应其 `MelonAgentFactory`）

```java
HarnessAgent.builder()
    .name(...).agentId(agentId)
    .toolkit(toolkit)                 // new Toolkit() + registerTool(实例)
    .workspace(Path)                  // 文件即配置
    .filesystem(LocalFilesystemSpec...)
    .middleware(middlewares)          // 有序中间件链
    .memory(MemoryConfig)             // flushTrigger / consolidation / retention
    .compaction(CompactionConfig)     // 窗口按比例 trigger/keep + summary model + truncateArgs
    .toolResultEviction(...)          // 按工具排除，控制工具结果占用
    .stateStore(new JsonFileAgentStateStore(...))
    .model(model).fallbackModel(...)  // 模型与工具各自 ExecutionConfig（timeout/重试/退避）
    .build();
```

- **工具**：普通 POJO + `@Tool(name, description, readOnly)` / `@ToolParam(...)`，`toolkit.registerTool(实例)`，按配置项开关启停 —— 无框架继承负担。
- **中间件**：实现 `MiddlewareBase.onAgent(agent, ctx, input, next) → Flux<AgentEvent>`；注意 Harness **自带** memory_search / memory_get / session_search 工具，注册时机早于业务中间件。
- **权限 / HITL**：AUTO / SMART / STRICT 三级；`builder.addAskRule(tool, PermissionRule(…, ASK, …))` 挂敏感工具（shell / write / edit / browser / spawn 等）。
- **上下文压缩**：约 128K 窗口，按比例 trigger / keep，支持 summary model、flush/offload-before-compact、参数截断。
- **记忆配置**：`MemoryConfig.flushTrigger(always())`、consolidationMaxTokens / consolidationMinGap、dailyFileRetentionDays / sessionRetentionDays —— **文件落地 + 定期整合**，非数据库。

### 3.2 Workspace「文件即配置」

工作区预置 6 个 md 模板 + 一组子目录与 JSON 元数据：

| 文件 | 作用 | 对应本项目 |
| :--- | :--- | :--- |
| `AGENTS.md` | 行为准则：安全边界 / 内外部操作 / 工具用法 | 根 `AGENTS.md` |
| `SOUL.md` | 灵魂：核心准则 / 边界 / 风格 / 连续性 | 人格设定 |
| `PROFILE.md` | 身份 + 用户资料（边走边更新） | PROFILE 构想 |
| `MEMORY.md` | 工具设置 + 经验教训小抄 | 记忆 M1 层雏形 |
| `BOOTSTRAP.md` | 首次「醒来仪式」，完成后自删 | persona 初始化可借鉴 |
| `HEARTBEAT.md` | 心跳轮询合并任务清单 | — |

子目录：`sessions/console`、`memory`、`mem_session`、`mem_metadata`、`digest`、`skills`、`.mcp`、`browser`、`media`、`tool_results`；元数据：`agent.json` / `chats.json` / `jobs.json` / `skill.json` / `credentials.yaml`。

### 3.3 参考价值小结

1. 它是**目前公开仓库中最贴近本项目形态的 AS 2.0 工程**，提供「多模块 + HarnessAgent 工厂装配 + workspace 初始化」的完整范式。
2. workspace「文件即配置」与本项目「AGENTS / handoff / 人格 / 记忆」文档体系天然同构，可平滑映射。
3. 工具 / 中间件 / 权限写法即 2.0 官方推荐姿势，Phase 1 可直接照此组织。

## 4. 对既有决策的冲击（需重验）

- 01 §5 定「记忆 M1–M3 自研（Redis/PG/pgvector）、AS Memory 仅桥接」。
- MelonPaw 实证：AS 2.0 Harness **自带 memory_search / memory_get / session_search** 工具（文件落地 + 检索），并用 **BM25** 覆盖默认 `memory_search`；旧式「主动注入记忆」中间件已废弃（其代码中 @Deprecated）。
- 🔴 **待 Phase 1 重验**：AS 2.0 自带记忆工具链的能力边界（检索质量、多会话整合、90/180 天留存）——若足够，自研范围可收窄到「向量检索 / 算法增强」等真正有价值的部分，而不是重复造基础记忆轮子。

## 5. 结论与引用注意

- ✅ 结论：作为 2.0 工程范式参考，采纳其 **HarnessAgent 工厂装配、workspace 初始化、工具/中间件/权限组织** 为 Phase 1 骨架蓝本之一。
- ⚠️ 引用注意：
  1. 版本差：它停在 **RC3**；本项目策略为 **2.0.2 实测、遇回归回退 2.0.0** —— 抄代码以本仓库实测版本 API 为准。
  2. WebFlux 全响应式是否适配本项目语音（半双工首版）需单独验证，不默认照搬。
  3. 前端（React 控制台）不在参考范围（前端由 AI 全权开发）。
- 🔴 遗留：§4 记忆边界重验结论需在 Phase 1 后回写本文件或 01。
