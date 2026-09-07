# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-08 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1.5（语义重构 + 物理结构整理）已完成编码与文档同步，待用户 review |
| 分支 | `main` |
| 当前架构 | 单 Maven 模块；Java 根包 `com.openalice`；分层为 `model / repository / agent(runtime|llm) / service / controller / dto / config`（ADR 09，修订 ADR 08） |
| Git 状态 | P1 SSE 实现与本轮 P1.5 重构均未 commit；旧路径显示删除、新 `com/` 路径未跟踪，属正常移动状态，待 `git add -A` 后可识别 rename |
| 验证 | 2026-09-08 全量测试通过：22 tests / 0 failures / 0 errors（本机 Java 17 临时覆盖 release；仓库目标仍是 Java 21） |

## 1. 用户最新确认（P1.5）

1. 在已确认的 P1.5 语义方案上继续，允许直接修改代码。
2. 物理结构同步优化：Java 根包改为 `com.openalice`，目录/文件按职责重新归位。
3. 参考本地 Jarvis 的优点，但不照搬：
   - Controller 薄、Service 编排；
   - 用户身份由服务端 / 上下文处理，不散落在请求协议；
   - Agent 调用前显式组装上下文。
4. 保持单 Maven 模块，不引入 Spring AI / Spring AI Alibaba。
5. 本轮不迁移到 PostgreSQL；M1 存储持久化另起任务。
6. Git 红线：不 commit、不 push；等待用户 review 与明确指示。

## 2. 当前架构

```text
OpenAlice/
├── pom.xml                      # 单模块 Spring Boot 应用，目标 Java 21
├── src/main/java/com/openalice/
│   ├── OpenAliceApplication.java
│   ├── model/
│   │   ├── ChatMessage.java
│   │   ├── UserId.java
│   │   ├── SessionId.java
│   │   ├── MessageRole.java
│   │   ├── ConversationTurn.java
│   │   └── TurnStatus.java
│   ├── repository/
│   │   ├── ConversationStore.java
│   │   └── memory/InMemoryConversationStore.java
│   ├── agent/
│   │   ├── AgentRequest.java
│   │   ├── AgentEvent.java
│   │   ├── TextDeltaEvent.java
│   │   ├── DoneEvent.java
│   │   ├── ErrorEvent.java
│   │   ├── runtime/
│   │   └── llm/
│   ├── service/
│   │   ├── ChatService.java
│   │   ├── ContextAssembler.java
│   │   └── SessionCoordinator.java
│   ├── controller/
│   ├── dto/
│   └── config/
├── src/test/java/com/openalice/  # 测试镜像新包结构
├── src/main/resources/application.yml
├── docs/
├── web/
└── handoff.md
```

依赖方向：

```text
model ← repository
model ← agent
service → model + repository + agent
controller → service
config 组装 repository + agent
```

核心边界：

- `model`：纯 POJO / 值对象，无框架与持久层依赖；
- `repository`：会话消息存储接口与实现；未来 PG 实现放 `repository.postgres`；
- `agent`：不读取 `ConversationStore`，只消费 `AgentRequest`、产出 `AgentEvent`；
- `service`：Turn 生命周期、上下文组装、持久化与 session 并发控制；
- `controller`：只做 HTTP / SSE 翻译；
- `config`：唯一组合根。

## 3. 本轮 P1.5 改动

### 3.1 物理结构

- Java 根包：`openalice` → `com.openalice`，测试同步镜像。
- `MemoryPort` → `ConversationStore`。
- `InMemoryMemoryPort` → `repository.memory.InMemoryConversationStore`。
- `MessageRole` 移入 `model`。
- `LlmProvider` 移入 `agent.llm`。
- 删除顶层 `enums/`，避免杂项包。
- Maven 依赖未新增，`pom.xml` 仅保持既有依赖与 Java 21 目标。

### 3.2 Turn 生命周期

新增：

- `com.openalice.model.ConversationTurn`
- `com.openalice.model.TurnStatus`

状态：

```text
RECEIVED → RUNNING → COMPLETED
                   → FAILED
RECEIVED/RUNNING → CANCELLED
```

当前 Turn 是进程内生命周期对象，未持久化；未来若需要审计，再扩展 `ConversationStore`，不提前加死接口。

### 3.3 单用户 API

```text
POST /api/v1/chat
{"sessionId":"default","message":"你好"}

GET /api/v1/sessions/{sessionId}/messages
```

- HTTP 请求 / 响应不暴露 `userId`；
- 服务端固定 `UserId.DEFAULT`；
- `ChatMessage.userId` 字段保留，为未来认证上下文注入预留；
- `ChatRequest` / `ChatStreamEvent` / `MessageView` 外部字段已同步去除 userId。

### 3.4 ContextAssembler

新增 `com.openalice.service.ContextAssembler`：

- 从 `ConversationStore` 读取最近 N 条业务历史；
- 校验当前 USER 消息一定在上下文中；
- 携带 system prompt；
- 生成不可变 `AgentRequest`；
- 配置项：`openalice.agent.context-window-size`，默认 20。

### 3.5 AgentRequest / AgentEvent

新增：

- `com.openalice.agent.AgentRequest`
- `com.openalice.agent.AgentEvent`
- `com.openalice.agent.TextDeltaEvent`
- `com.openalice.agent.DoneEvent`
- `com.openalice.agent.ErrorEvent`

`AgentRuntime` 接口：

```java
Flux<AgentEvent> stream(AgentRequest request);
```

`AgentScopeAgentRuntime` 每次：

1. 根据Turn 构建 `RuntimeContext`；
2. `agent.clearContext(context)`；
3. 将 `AgentRequest.conversationContext` 转成 AgentScope messages；
4. 调用 `agent.streamEvents(messages, context)`；
5. 输出 `TextDeltaEvent / DoneEvent / ErrorEvent`。

注意：AgentScope 2.0.2 不允许 hook 将 SYSTEM message 注入 input messages；system prompt 仍通过 `HarnessAgent.builder().sysPrompt(...)` 设置。业务历史唯一真相源是 `ConversationStore`，AgentScope state 只是运行态 scratch。

### 3.6 SessionCoordinator

新增 `com.openalice.service.SessionCoordinator`：

- 同一 `sessionId` 的完整 turn 串行执行；
- 不同 `sessionId` 可并行；
- 串行范围覆盖 USER append、history 读取、AgentRequest 组装、Agent 调用、ASSISTANT append、Turn 状态转换；
- 使用 per-session `Semaphore(1)` 与 `Flux.usingWhen`，完成 / 错误 / 取消都会释放。

当前不为 semaphore map 做复杂清理，避免小规模单用户场景下引入竞态；未来 session 数量显著增大时再评估安全清理。

## 4. 测试状态

2026-09-08 验证结果：

```text
22 tests passed
0 failures
0 errors
```

测试分布：

```text
model: 4
repository.memory: 4
agent.runtime: 3
service: 8
controller: 3
```

新增 / 更新测试：

- `com.openalice.model.ConversationTurnTest`
- `com.openalice.repository.memory.InMemoryConversationStoreTest`
- `com.openalice.service.ContextAssemblerTest`
- `com.openalice.service.SessionCoordinatorTest`
- `com.openalice.service.ChatServiceTest`
- `com.openalice.agent.runtime.AgentScopeAgentRuntimeTest`
- `com.openalice.controller.ChatControllerTest`
- `com.openalice.model.ChatMessageTest`

验证说明：

- 当前机器只有 Java 17，因此测试时临时使用 `-Dmaven.compiler.release=17`；
- Mockito 在该 JDK 组合下需显式挂 Byte Buddy agent；
- 仓库目标仍是 Java 21，需在 Java 21 环境复跑标准 `mvn clean test`；
- 真实进程 SSE 冒烟未执行；测试已覆盖 MockMvc async dispatch 下的完整 `text_delta → done` 链路。

## 5. 文档状态

已同步：

- `docs/decisions/09-p15-semantic-and-package-structure.md`
- `docs/CONTEXT.md`
- `docs/index.md`
- `docs/代码学习导览 v0.1.md`（内容 v0.5）
- `README.md`
- `AGENTS.md`
- `handoff.md`
- `src/main/resources/application.yml`

旧历史 ADR / 需求书中保留当时的 `MemoryPort`、旧包路径和旧 API 表述，这是历史记录，不需要整体改写；当前有效架构以 ADR 09 与本文件为准。

## 6. 红线与约定

- Java 21 目标；Spring Boot 3.5.16；AgentScope Java 2.0.2。
- 单 Maven 模块；不引入 Spring AI / Spring AI Alibaba。
- 不做 PostgreSQL M1；当前仅 in-memory。
- 不提前创建空包 / 空模块 / 无人引用死代码。
- `web/` 不进入 Maven。
- 公开安全：不提交真实 key、个人邮箱、本机绝对路径、组织信息、构建产物。
- Git：commit 需用户明确确认；push 一律用户手动执行。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。

## 7. 下一步

1. 用户 review 当前 diff；
2. review 确认后执行 `git add -A`，让旧路径删除 + 新路径新增被识别为 rename；
3. 用户明确指示后本地 commit（建议 message 见下）；
4. 在 Java 21 环境复跑标准 `mvn clean test`；
5. 如需真实验证，启动进程并用 `curl -N` 冒烟 SSE；
6. 下一个架构任务：M1 PostgreSQL `session_message` 持久化；
7. M1 后做 persona 首启初始化。

### 建议 commit message

```text
refactor(architecture): 完成 P1.5 语义与包结构整理

- Java 根包迁移至 com.openalice，测试镜像同步
- MemoryPort 重命名为 ConversationStore，实现迁移至 repository.memory
- 引入 ConversationTurn / TurnStatus 与显式 Turn 生命周期
- HTTP API 移除 userId，服务端固定单用户身份
- 新增 ContextAssembler，Agent 调用前显式组装上下文
- AgentRuntime 改为 Flux<AgentEvent>，Controller 仅负责 SSE 翻译
- 新增 SessionCoordinator，同 session 串行、不同 session 并行
- 同步 ADR 09、README、AGENTS、CONTEXT、学习导览与测试
```

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
