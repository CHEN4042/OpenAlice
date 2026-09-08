<div align="center">

# A.L.I.C.E.

### 爱丽丝

*(/ˈælɪs/ · Alice)*

*一个倾听的、共情的、温柔的、永不忘记你的存在。*
*A lifelong AI companion who never forgets.*

![Status](https://img.shields.io/badge/status-phase%201.5-blue)
![JDK](https://img.shields.io/badge/JDK-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot)
![AgentScope Java](https://img.shields.io/badge/AgentScope%20Java-2.0.2-9cf)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

---

## 项目简介

面向**唯一用户本人**的 AI 陪伴助手 —— 情绪陪伴 + 深度交互，**记忆是灵魂**。

P1.5（语义整理）进行中：`HTTP/SSE → ChatService → ContextAssembler → AgentScope → ConversationStore` 链路已整理为显式 Turn 与 AgentEvent；P1 目标为真实模型双 provider、SSE 流式、M1 会话消息 PostgreSQL 持久化与单用户 `persona/` 初始化。

## 架构

单 Spring Boot 模块（`io.openalice:openalice`），源码根包 `com.openalice`：

```text
OpenAlice/
├── pom.xml                         # 单模块应用工程
├── src/main/java/com/openalice/
│   ├── OpenAliceApplication.java   # 启动类（Spring 组合根）
│   ├── model/                      # 共享词汇：ChatMessage / MessageRole（纯 LLM 消息）
│   ├── chat/                       # ★ 业务：对话（业务包，内部按类型整理）
│   │   ├── service/                # ChatService · ContextAssembler · SessionCoordinator
│   │   └── store/                  # ConversationStore · StoredMessage · memory/ 内存实现
│   ├── agent/
│   │   ├── AgentRequest.java       # 显式输入：userId/sessionId + 上下文
│   │   ├── AgentEvent.java         # TextDelta / Done / Error 事件
│   │   ├── runtime/                # AgentRuntime + AgentScope 适配器
│   │   └── llm/                    # 双 provider 模型接入
│   ├── controller/                 # HTTP/SSE 翻译
│   ├── dto/                        # API 出入参
│   └── config/                     # Spring 组合根
├── web/                            # 未来前端占位，不进入 Maven
└── docs/
```

一次 `/chat` 的职责链：

```text
ChatController          # HTTP/SSE 翻译，不写业务
  → ChatService        # Turn 生命周期 + USER/ASSISTANT 持久化
    → SessionCoordinator  # 同 session 串行，不同 session 并行
    → ContextAssembler   # 从 ConversationStore 读取最近 N 条，生成 AgentRequest
    → AgentRuntime       # clearContext 后传显式上下文，返回 Flux<AgentEvent>
```

`ConversationStore` 是业务历史唯一真相源；AgentScope state store 只是运行态 scratch，不承担业务记忆。

## 构建与运行

要求：Java 21+、Maven 3.9+。

```bash
mvn clean test
mvn package
mvn spring-boot:run
```

### LLM 接入（配置文件）

未配置任何 key 时自动回落到确定性 mock（回复形如 `收到：你好`）；配置后走真实模型。

公共默认值写在 `src/main/resources/application.yml`（**不含 key**），相关键：

| 键 | 说明 | 默认 |
| :--- | :--- | :--- |
| `openalice.agent.system-prompt` | 系统提示词 | Alice 默认提示词 |
| `openalice.agent.context-window-size` | 携带的历史消息条数 | `20` |
| `openalice.llm.provider` | `auto` / `mock` / `deepseek` / `agentrouter` | `auto`：中转 → DeepSeek → mock |
| `openalice.llm.model` | 模型名覆盖 | provider 默认 |
| `openalice.llm.base-url` | API Base URL 覆盖 | provider 默认 |
| `openalice.llm.proxy` | HTTP 代理 `host:port` | 不代理 |
| `openalice.llm.api-key` | **真实 key——绝不提交** | 仅 local / env |

**本机真实 key 放 gitignored 的 `application-local.yml`**（仓库已提供模板），然后以 `local` profile 启动：

```bash
# 1) 编辑 src/main/resources/application-local.yml，把 api-key 换成你的真实 key（建议同时显式指定 provider）
# 2) 启动时激活 local profile：
mvn spring-boot:run -Dspring-boot.run.profiles=local
# IDEA：Run Configuration → Active profiles 填 local（见下）
```

环境变量仍可作为兜底覆盖（Spring 会把 `OPENALICE_LLM_PROVIDER` 映射到 `openalice.llm.provider`，key 变量 `OPENALICE_DEEPSEEK_API_KEY` / `OPENALICE_AGENTROUTER_API_KEY` 依旧生效），但不是主路径。

## API

### 聊天（SSE）

```bash
curl -N -X POST http://localhost:8080/api/v1/chat \
  -H 'Accept: text/event-stream' \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"default","message":"你好"}'
```

```text
event:text_delta
data:{"type":"text_delta","sessionId":"default","delta":"收到：你好"}

event:done
data:{"type":"done","sessionId":"default","reply":"收到：你好"}
```

### 历史消息

```bash
curl http://localhost:8080/api/v1/sessions/default/messages
```

## 当前边界

- LLM 已接入 AgentRouter 优先 + DeepSeek 官方兜底，无 key 自动回落 mock；
- 当前使用 `InMemoryConversationStore`，进程重启不保留；M1 将替换为 PostgreSQL `session_message`；
- 语音、learning、插件、多 Agent 只保留架构位置，暂不实现；
- `web/` 只是占位，不进入 Maven Reactor。

## 文档导航

| 文档 | 说明 |
| :--- | :--- |
| [docs/index.md](docs/index.md) | 文档总索引 |
| [handoff.md](handoff.md) | 会话交接 |
| [docs/decisions/10-package-layout-evolution-rules.md](docs/decisions/10-package-layout-evolution-rules.md) | 当前包结构与演进规则（ADR 10） |
| [docs/decisions/09-p15-semantic-and-package-structure.md](docs/decisions/09-p15-semantic-and-package-structure.md) | P1.5 语义决议（顶层布局已被 ADR 10 修订） |
| [docs/decisions/07-p1-base-decisions.md](docs/decisions/07-p1-base-decisions.md) | P1 底座决策 |
| [docs/CONTEXT.md](docs/CONTEXT.md) | 术语速查 |
