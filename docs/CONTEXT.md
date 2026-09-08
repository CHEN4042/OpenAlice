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
- 构建：**Maven 单模块**（ADR 08，修订 ADR 06）：根 `pom.xml` 即 Spring Boot 应用（`io.openalice:openalice`），根包 `com.openalice`，顶层包分层 `model / agent / llm / chat(service|store) / controller / dto / config`（ADR 10，修订 ADR 09 顶层布局；`runtime/`、`agent.llm` 已在本轮拆除，`llm` 上提为顶层包）。
- LLM（真实，P1 起）：**中转站（OpenAI 兼容，优先）+ DeepSeek 官方 API（兜底）**，双 provider 故障自动切换；`auto` 解析 = 中转（AgentRouter）有 key 优先 → DeepSeek 兜底 → 都无 key 直接启动/调用报错（mock 回退已移除，真实 key 见 ADR 11 与本机 local 配置）。配置默认值在 `application.yml`（`openalice.agent.*` / `openalice.llm.*`，Spring 绑定见 `config.OpenAliceSettings`）；**真实 key 只放 gitignored `application-local.yml`（`--spring.profiles.active=local`）或 `OPENALICE_*` 环境变量**。代码见 `com.openalice.llm.LlmModelFactory`（顶层 `llm` 包）。
- P1.5 会话存储：当前 `InMemoryConversationStore`（过渡）；M1 目标 = 会话消息实时落 PostgreSQL（`session_message`），AgentScope 运行态单实例**进程内**；**Redis 已砍、预留后置**（需求书 v1.5 / ADR 07）。
- Agent 不读取存储；`chat.service.ContextAssembler` 从 `chat.store.ConversationStore` 组装显式 `AgentRequest`，AgentScope state 仅作运行态 scratch。
- 单用户：HTTP API 不暴露 `userId`，服务端固定默认用户（`ChatService` 私有常量）；`ChatMessage` 纯 role/content，归属与时间戳下沉到 `chat.store.StoredMessage`（id / userId / sessionId / createdAt），存储行保留 `userId` 供未来认证（ADR 13）。首次启动引导初始化 `persona/` 文件（user.md 等，参考 OpenHanako）仍待实现。
- 文本通道：`/chat` 走 HTTP + SSE 流式（P1 起）；WebSocket 双向通路留给 P3 语音。
- 语音、learning、插件、多 Agent 仅保留架构位置：语音 P3、Web / 工具 / 主动 P4，P1 不实现。

## 高频术语

| 术语 | 含义 |
| :-- | :-- |
| M1–M3（记忆） | M1 短期 → M2 结构化 → M3 语义；必须自研掌控 |
| L1/L2（隐私） | L1 可上云；L2 仅本地 |
| P1–P4 / 暂缓 | 阶段坐标（P = Phase）：P1 会说真话（底座）→ P2 记得住（记忆）→ P3 听得到（语音）→ P4 看得见 / 摸得到；「暂缓」= 无明确排期 |
| 组合根 | `config.OpenAliceConfiguration`，唯一手写装配点（Spring `@Configuration`） |
| 会话存储 | `chat.store.ConversationStore`：业务会话历史 append / history 抽象 |
| 全双工语音 | 目标形态：边听边说、可打断；P3 技术验证目标，不计入阶段验收 |

## 行文与编号纪律

- 记忆 = M1–M3；隐私 = L1/L2；阶段坐标 = P1–P4 / 暂缓（P = Phase，v1.5 起废弃功能优先级 P0/P1/P2 旧标记），禁止混用。
- 参考项目与需求书只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。
