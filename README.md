<div align="center">

# A.L.I.C.E.

### 爱丽丝

*(/ˈælɪs/ · Alice)*

*一个倾听的、共情的、温柔的、永不忘记你的存在。*
*A lifelong AI companion who never forgets.*

![Status](https://img.shields.io/badge/status-phase%201-blue)
![JDK](https://img.shields.io/badge/JDK-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot)
![AgentScope Java](https://img.shields.io/badge/AgentScope%20Java-2.0.2-9cf)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

---

## 项目简介

面向**唯一用户本人**的 AI 陪伴助手 —— 情绪陪伴 + 深度交互，**记忆是灵魂**。

P1（会说真话 · 底座）进行中：`HTTP → AgentScope → MemoryPort` 最小链路已跑通（in-memory 过渡）；P1 目标为接入真实模型（**中转站优先 + DeepSeek 官方兜底**）、`/chat` SSE 流式、M1 会话消息 PG 持久化与单用户 `persona/` 初始化。

## 架构

单 Spring Boot 模块（`io.openalice:openalice`），源码根包 `openalice`，按职责分层：

```text
OpenAlice/
├── pom.xml                    # 单模块应用工程
├── src/main/java/openalice/
│   ├── OpenAliceApplication.java   # 启动类（组合根 = Spring 容器）
│   ├── model/                      # 纯 POJO：ChatMessage · UserId · SessionId …
│   ├── port/                       # 端口接口：MemoryPort
│   ├── memory/                     # 记忆实现：InMemoryMemoryPort（将来换 PostgreSQL）
│   ├── agent/runtime + llm/        # AgentScope 执行器 + LLM 模型接入
│   ├── service/                    # ChatService：一次 /chat 的业务编排
│   ├── controller/ dto/            # HTTP 入口与出入参
│   └── config/                     # Spring 组合根（换存储只改这里）
├── web/                      # 未来前端占位，不进入 Maven
└── docs/                     # 文档索引 docs/index.md 入口
```

一次 `/chat` 的职责链：`controller`（只接 HTTP）→ `ChatService`（编排：先存用户消息 → 问 agent → 存回复）→ `agent.runtime`（只问模型一次）→ `MemoryPort`（存哪由实现决定）。`agent` 不依赖记忆实现，只面向 `port.MemoryPort`。

## 构建与运行

要求：Java 21+、Maven 3.9+。

```bash
# 全量测试
mvn clean test

# 打包可执行 jar
mvn package

# 本地启动
mvn spring-boot:run
```

### LLM 接入（环境变量）

未配置任何 key 时自动回落到确定性 mock（回复形如 `收到：你好`）；配置后走真实模型。
`agent` 模块按以下环境变量解析，key 只经环境变量传入、不落盘不入库：

| 变量 | 说明 | 默认 |
| :-- | :-- | :-- |
| `OPENALICE_LLM_PROVIDER` | `auto` / `mock` / `deepseek` / `agentrouter` | `auto`：有中转 key 走中转 → 有 DeepSeek key 走官方 → 都无则 mock |
| `OPENALICE_AGENTROUTER_API_KEY` | AgentRouter 中转站 key（优先） | — |
| `OPENALICE_DEEPSEEK_API_KEY` | DeepSeek 官方 key（兜底） | — |
| `OPENALICE_LLM_MODEL` | 模型名覆盖 | `deepseek-v4-flash` |
| `OPENALICE_LLM_BASE_URL` | API Base URL 覆盖 | 官方 `https://api.deepseek.com` / 中转 `https://agentrouter.org` |
| `OPENALICE_LLM_PROXY` | HTTP 代理 `host:port`（中转站需走代理时设置） | 不代理 |

示例（中转站）：

```bash
OPENALICE_LLM_PROVIDER=agentrouter \
OPENALICE_AGENTROUTER_API_KEY=... \
OPENALICE_LLM_PROXY=127.0.0.1:7897 \
mvn spring-boot:run
```

> 说明：AgentRouter 中转站有 WAF，只放行带 Codex 客户端指纹头的请求；`agent` 内部
> 的 `ConfiguredHttpTransport` 会自动注入该指纹头，因此 Java 侧可直接对接，无需额外反代。

测试接口：

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user","sessionId":"default","message":"你好"}'
```

预期响应：

```json
{
  "userId": "user",
  "sessionId": "default",
  "reply": "收到：你好"
}
```

> 注：上述为未配置 LLM key（mock 模式）的响应；配置真实 key 后返回模型真实输出。

## 当前边界

- LLM 已接入真实双 provider（AgentRouter 中转站优先 + DeepSeek 官方兜底），未配置 key 自动回落 mock；模型层已流式（`stream=true`），HTTP `/chat` SSE 客户端流式留待后续；
- 当前使用内存存储（进程重启不保留）；P1 底座目标 = M1 会话消息落 PostgreSQL `session_message`（重启不丢）；Redis 已砍、预留后置；
- 语音、learning、插件、多 Agent 只保留架构位置，暂不实现；
- `web/` 只是占位，不进入 Maven Reactor。

## 文档导航

| 文档 | 说明 |
| :-- | :-- |
| [docs/index.md](docs/index.md) | 文档总索引：先读它，按需选读 |
| [handoff.md](handoff.md) | 会话交接：下一位 AI / 协作者先读 |
| [docs/项目需求说明书 v1.2.md](docs/项目需求说明书%20v1.2.md) | 项目需求说明书 v1.5（核心规格） |
| [docs/decisions/08-single-module-convergence.md](docs/decisions/08-single-module-convergence.md) | 当前架构决议：单模块收敛 + 顶层包分层 |
| [docs/decisions/07-p1-base-decisions.md](docs/decisions/07-p1-base-decisions.md) | P1 底座五项技术决策 |
| [docs/decisions/05-architecture-naming-evolution.md](docs/decisions/05-architecture-naming-evolution.md) | 架构蓝图 v4（顶层布局已被 ADR 08 修订） |
| [docs/CONTEXT.md](docs/CONTEXT.md) | 共享语言与术语速查 |
| [docs/decisions/](docs/decisions/) | 技术决策记录（ADR） |
