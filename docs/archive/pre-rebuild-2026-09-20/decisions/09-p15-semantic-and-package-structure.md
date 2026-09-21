# ADR 09 · P1.5 语义重构与包结构整理

- 状态：✅ 当前有效
- 日期：2026-09-07
- 关系：修订 ADR 08 的根包与顶层目录细节；保留其“单 Maven 模块 + service 编排”的核心结论

## 1. 背景

P1 SSE 链路已经可用，但语义上仍有四类不顺手：

1. `MemoryPort` 名字过宽，实际承诺只是“会话消息存储”；
2. controller / service / runtime 之间传递 `Flux<String>`，SSE 事件与业务事件耦合；
3. 上下文依赖 AgentScope 内部 state，容易与业务历史形成双份上下文；
4. 根包 `openalice` 缺少常规 Java 命名空间，`enums`、`port`、`memory` 的物理位置也不够直观。

本地 Jarvis 的可取之处是：userId 从上下文注入、service 负责业务编排、Agent 调用前显式组装上下文；但它没有照搬的价值点包括平铺大包与请求内 userId 语义。OpenAlice 保留单模块，但用自己的更小边界实现。

## 2. 决策

### 2.1 根包与物理结构

根包改为 `com.openalice`，测试镜像相同包结构：

```text
src/main/java/com/openalice/
├── OpenAliceApplication.java
├── model/                      # ChatMessage · UserId · SessionId · ConversationTurn · TurnStatus · MessageRole
├── repository/
│   ├── ConversationStore.java  # 会话存储接口
│   └── memory/                 # InMemoryConversationStore；未来 postgres/ 放同级
├── agent/
│   ├── AgentRequest.java
│   ├── AgentEvent.java · TextDeltaEvent · DoneEvent · ErrorEvent
│   ├── runtime/                # AgentRuntime · AgentScope 适配器 · factory · properties
│   └── llm/                    # LlmProvider · model factory · deterministic model · transport
├── service/                    # ChatService · ContextAssembler · SessionCoordinator
├── controller/                 # HTTP/SSE 翻译
├── dto/                        # ChatRequest · ChatStreamEvent · MessageView
└── config/                     # Spring 组合根
```

`MessageRole` 是消息模型的一部分，放入 `model`；`LlmProvider` 只被模型接入层使用，放入 `agent.llm`。不再保留单独的 `enums/` 顶层包。

### 2.2 单用户 API

外部 API 不暴露 userId：

```text
POST /api/v1/chat
{"sessionId":"default","message":"你好"}

GET /api/v1/sessions/{sessionId}/messages
```

服务端固定使用 `UserId.DEFAULT`。存储模型继续保留 `userId` 字段，为未来认证上下文注入预留。

### 2.3 Turn 与上下文

- `ConversationTurn` + `TurnStatus` 显式表达 RECEIVED / RUNNING / COMPLETED / FAILED / CANCELLED；
- `ContextAssembler` 在调用 Agent 前从 `ConversationStore` 读取最近 N 条消息，并生成 `AgentRequest`；
- `ConversationStore` 是业务会话历史唯一真相源；
- AgentScope state store 只是运行态 scratch，每次调用前 `clearContext()`，避免双历史。

### 2.4 AgentEvent 与 SSE

`AgentRuntime.stream(AgentRequest)` 返回 `Flux<AgentEvent>`：

- `TextDeltaEvent`
- `DoneEvent`
- `ErrorEvent`

`ChatService` 负责持久化与 Turn 生命周期；`ChatController` 只把 `AgentEvent` 翻译为 `ChatStreamEvent` / `ServerSentEvent`。

### 2.5 SessionCoordinator

同一个 `sessionId` 的完整 turn 串行执行；不同 `sessionId` 可并行。串行范围覆盖：

```text
append USER
read history
assemble AgentRequest
call Agent
append ASSISTANT
transition Turn
```

不依赖 AgentScope 内部串行化来保证业务一致性。

## 3. 影响

- Maven 依赖不变；
- P1 外部协议从 request/response 中移除 userId；
- 未来 PostgreSQL 实现应命名为 `repository.postgres` 下的 `ConversationStore` 实现；
- 测试新增 Turn、ContextAssembler、SessionCoordinator，并同步更新 agent/service/controller/repository 测试。

## 4. 后续

1. M1 将 `InMemoryConversationStore` 替换为 PostgreSQL 实现；
2. Turn 目前是进程内生命周期对象，若未来需要审计，再扩展 `ConversationStore`，不提前加死接口；
3. 若上下文窗口需要配置化，可通过 `openalice.agent.context-window-size` 调整。
