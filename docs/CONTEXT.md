# CONTEXT.md · 共享语言与术语速查

> 目的：给 Codex / 协作者一个最小共识词汇表，避免每次用长句重述概念。
> 性质：速查索引，不是规格。以《项目需求说明书》与 `docs/decisions/` 最新决策为准。

## 一句话定位

A.L.I.C.E.（爱丽丝）= 面向**唯一用户本人**的 AI 陪伴助手 —— “一个倾听的、共情的、温柔的、永不忘记你的存在”。情绪陪伴 + 深度交互，**记忆是灵魂**。

## 命名双层体系

| 层级 | 名称 | 使用场景 |
| :-- | :-- | :-- |
| ① 工程代号 | **openalice** | GitHub 仓库名（OpenAlice）、Maven artifact、Java 根包 |
| ② 全称 | **A.L.I.C.E.**（爱丽丝 · Alice，/ˈælɪs/） | Logo、README 标题、AI 自称 |

> A.L.I.C.E. 非首字母缩写；点分形态仅为仪式 / Logo 呈现。人格表述与名字解耦。

## 当前技术结论

- Agent 框架：AgentScope Java 2.0.2，Phase 1 实测；如关键回归可回退 2.0.0。
- Web 壳：Spring Boot 3.5.16，仅用于 HTTP / 装配，不引入 Spring AI。
- Java：21。
- 构建：**Maven 单模块**（ADR 08，修订 ADR 06）：根 `pom.xml` 即 Spring Boot 应用（`io.openalice:openalice`），根包 `openalice`，顶层包分层 `model / port / memory / agent(runtime|llm) / service / controller / dto / config`。
- LLM（真实，P1 起）：**中转站（OpenAI 兼容，优先）+ DeepSeek 官方 API（兜底）**，双 provider 故障自动切换；key 走 `OPENALICE_*` 环境变量；`auto` 回退链 = 中转 → DeepSeek → 无 key 回落 `DeterministicChatModel`（mock），代码见 `openalice.agent.llm.LlmModelFactory`。
- Phase 1 记忆：当前 `InMemoryMemoryPort`（过渡）；P1 目标 = M1 会话消息实时落 PostgreSQL（`session_message`），AgentScope 运行态单实例**进程内**；**Redis 已砍、预留后置**（需求书 v1.5 / ADR 07）。
- Agent 不直接依赖 memory 实现，只依赖 `port.MemoryPort`（单模块后 runtime 连 MemoryPort 都不持有，编排收敛到 `service.ChatService`）。
- 单用户：固定唯一用户，`user_id` 仅作存储预留、不作路由键；首次启动引导初始化 `persona/` 文件（user.md 等，参考 OpenHanako）。
- 文本通道：`/chat` 走 HTTP + SSE 流式（P1 起）；WebSocket 双向通路留给 P3 语音。
- 语音、learning、插件、多 Agent 仅保留架构位置：语音 P3、Web / 工具 / 主动 P4，P1 不实现。

## 高频术语

| 术语 | 含义 |
| :-- | :-- |
| M1–M3（记忆） | M1 短期 → M2 结构化 → M3 语义；必须自研掌控 |
| L1/L2（隐私） | L1 可上云；L2 仅本地 |
| P1–P4 / 暂缓 | 阶段坐标（P = Phase）：P1 会说真话（底座）→ P2 记得住（记忆）→ P3 听得到（语音）→ P4 看得见 / 摸得到；「暂缓」= 无明确排期 |
| 组合根 | `config.OpenAliceConfiguration`，唯一手写装配点（Spring `@Configuration`） |
| 端口 | `port/` 包中的接口抽象（如 `MemoryPort`） |
| 全双工语音 | 目标形态：边听边说、可打断；P3 技术验证目标，不计入阶段验收 |

## 行文与编号纪律

- 记忆 = M1–M3；隐私 = L1/L2；阶段坐标 = P1–P4 / 暂缓（P = Phase，v1.5 起废弃功能优先级 P0/P1/P2 旧标记），禁止混用。
- 参考项目与需求书只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。
