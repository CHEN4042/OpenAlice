# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-08 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | HEAD `17af011`（ADR 10）；工作区含 **ADR 11（LLM 配置 local profile）+ ADR 12（id String 化）+ ADR 13（ChatMessage 职责拆分与冗余清理）** 三轮改动，编码 + 文档已完成，**等用户 review 后 commit**（用户已示意直接提交，尚未执行） |
| 分支 | `main` |
| 当前架构 | 单 Maven 模块；根包 `com.openalice`；顶层 `model / agent(runtime\|llm) / chat(service\|store) / controller / dto / config`（ADR 10） |
| Git 状态 | 工作区含 ADR 11 + 12 + 13 改动，**均未 commit**；`application-local.yml` 已 gitignore，不会入库 |
| 验证 | ADR 13 重构主代码 `mvn compile` 通过；**测试未在本会话运行**（用户明确「不需要测试了，直接提交」），测试代码已同步但未验证 |

## 1. 用户最新确认

### 1.1 ADR 11 · LLM 配置（沿用上一轮结论）

1. LLM 配置默认值全部进 `application.yml`（提交、公共安全）；本机真实 key 走 **gitignored `application-local.yml`**，`--spring.profiles.active=local` 激活（取 `local` 不取 `dev`）。
2. key 红线：真实 key 绝不进提交文件；local 模板已 gitignore。

### 1.2 ADR 12 · UserId / SessionId 降为 String（已被 ADR 13 进一步收敛）

删除两个值对象，id 直接裸 `String` 字段；`UserId.DEFAULT` → `ChatMessage.DEFAULT_USER_ID`。ADR 13 又把它从消息上拿掉、下沉存储行（见下）。

### 1.3 ADR 13 · ChatMessage 职责拆分与全项目冗余清理（本轮）

用户原话：「你的 `ChatMessage` 为什么还是这么抽象？里面挤得的东西也太多了吧…」「重构，顺便检查别的文件，感觉有挺多都过于冗余了」。

核心决策（详见 `docs/decisions/13-chat-message-responsibility-split.md`）：

- `model/ChatMessage` 收敛为纯 LLM 消息 `record(MessageRole role, String content)`（只留 `user(content)` / `assistant(content)` 工厂 + 校验）；
- 新增 `chat/store/StoredMessage`：存储行 `(id, userId, sessionId, ChatMessage message, createdAt)`，归属与时间戳下沉；
- `ConversationStore` 接口改 `append(StoredMessage)` / `history(String sessionId, int limit)`（去掉 userId 入参）；
- `MessageRole` 增 `wireValue()`（"user"/"assistant"/"system"）消除 `name().toLowerCase()` 重复；
- **删除 `ConversationTurn` / `TurnStatus`**：10 字段进程内状态机运行期零消费（`currentTurn` 只 set 不读、不落库），AgentScope 只需 userId/sessionId——ADR 09 §4.2 自述「不提前加死接口」；
- `AgentRequest` 改为直接携带 `String userId / sessionId + context + systemPrompt`；
- 删除无消费者 blocking：`ChatService.chat()` / `AgentRuntime.chat()` 及仅服务它们的 `dto/ChatResponse` / `agent/runtime/ChatResult`；
- `ChatStreamEvent` 移除冗余 `sessionId` 字段；`ChatController` 双 if-else（`eventName`+`toStreamEvent`）收敛为单个 switch（SSE event name 用 `type()`）；
- `dto/MessageView` 变纯 record，映射收进 `ChatService.history()`（避免 dto 反向依赖 chat.store）；
- `ChatMessage.DEFAULT_USER_ID` → `ChatService` 私有常量 `DEFAULT_USER_ID = "openalice-user"`。

## 2. 当前架构（ADR 10 + ADR 13 后）

```text
OpenAlice/
├── pom.xml                      # 单模块 Spring Boot 应用，目标 Java 21
├── src/main/java/com/openalice/
│   ├── OpenAliceApplication.java
│   ├── model/                   # 共享词汇：ChatMessage(role/content) · MessageRole（ADR 13 最小化）
│   ├── agent/                   # 内核：AI 调用
│   │   ├── AgentRequest.java(userId/sessionId/context/systemPrompt) · AgentEvent · TextDeltaEvent · DoneEvent · ErrorEvent
│   │   ├── runtime/             # AgentRuntime(stream) · AgentScopeAgentRuntime(适配器) · Factory · Properties
│   │   └── llm/                 # LlmProvider · LlmModelFactory · DeterministicChatModel · ConfiguredHttpTransport
│   ├── chat/                    # ★ 业务：对话（ADR 10 业务包，内部按类型整理）
│   │   ├── service/             # ChatService(DEFAULT_USER_ID) · ContextAssembler · SessionCoordinator
│   │   └── store/               # ConversationStore · StoredMessage · memory/InMemoryConversationStore
│   ├── controller/              # ChatController · HealthController · ApiExceptionHandler
│   ├── dto/                     # ChatRequest · ChatStreamEvent(type/delta/reply/error) · MessageView
│   └── config/                  # 组合根：OpenAliceConfiguration · OpenAliceSettings(@ConfigurationProperties)
├── src/test/java/com/openalice/  # 测试镜像 main 包结构
├── src/main/resources/application.yml        # 公共默认（无 key）
├── src/main/resources/application-local.yml # 本机私有覆盖含 key（gitignored）
├── docs/
├── web/
└── handoff.md
```

依赖方向：

```text
model ← chat.store（StoredMessage 持 ChatMessage）
model ← agent
chat.service → model + chat.store + agent + dto
controller → chat.service + dto
config 组装 chat.store + agent.runtime
```

核心边界：

- `model`：只有跨 chat/agent 的真共享物（ChatMessage / MessageRole），纯 POJO，无框架与持久层依赖；
- `chat.store`：存储行 StoredMessage + 会话历史唯一真相源（端口 + 内存实现）；
- `chat.service`：单 turn 编排（无 Turn 状态机）、上下文组装、持久化与 session 并发控制；`DEFAULT_USER_ID` 私有；
- `agent`：不读 ConversationStore，只消费 AgentRequest、产出 AgentEvent；
- `controller`：只做 HTTP/SSE 翻译；`config`：唯一组合根；
- 未来 `memory` / `persona` 等新业务按 ADR 10 §4 同级新建业务包，不提前建空包。

## 3. 本轮改动文件（ADR 11 + 12 + 13，未 commit）

### 3.1 ADR 11（配置）
- 默认值移入 `application.yml`（openalice.agent.* / openalice.llm.*）；新增 gitignored `application-local.yml`；
- `AgentRuntimeProperties` 收敛为纯配置容器；新增 `config.OpenAliceSettings`；
- `LlmModelFactory` key 解析：local api-key 优先，`OPENALICE_*_API_KEY` env 兜底。

### 3.2 ADR 12（id String 化）
- 删除 `model/UserId.java`、`model/SessionId.java`；id 字段即语义。

### 3.3 ADR 13（消息拆分 + 冗余清理）
- 新增 `chat/store/StoredMessage.java`；`ChatMessage`/`MessageRole` 收敛；`ConversationStore` 接口按 sessionId 收口；
- 删除 `ConversationTurn` / `TurnStatus` / `ChatResult` / `ChatResponse`；
- `AgentRequest`、`AgentRuntime`、`ContextAssembler`、`ChatService`、`ChatController`、`ChatStreamEvent`、`MessageView` 同步；
- 测试同步：删 `ConversationTurnTest`，`ChatMessageTest` / `InMemoryConversationStoreTest` / `ContextAssemblerTest` / `ChatServiceTest` / `AgentScopeAgentRuntimeTest` 改新 API；`ChatControllerTest` 未改（协议断言不变）。

## 4. 测试状态

- ADR 13 主代码 `mvn -q compile` 通过；
- **本会话未运行 `mvn clean test`**（用户打断并说「不需要测试了，直接提交吧」）。测试文件已按新 API 同步，预估 21 个测试；**下次接手第一件事：跑 `mvn clean test` 验证**（可能有漏网编译/断言问题，尤其是 `ChatControllerTest` 的 SSE data 断言——`ChatStreamEvent` 已去掉 sessionId 字段）。

## 5. 文档状态（本会话已同步 ADR 13）

- 新增 `docs/decisions/13-chat-message-responsibility-split.md`；
- `AGENTS.md`（目录树 model/store/AgentRequest 行、规则行、阅读顺序）
- `docs/index.md`（ADR 13 表格行 + 阅读顺序 + 整理记录）
- `docs/CONTEXT.md`（单用户 bullet）
- `README.md`（目录树）
- `docs/代码学习导览 v0.1.md`（升 v0.8：目录树、4.1/4.2/4.4/4.5/4.6、§6/§7、变更记录）
- `handoff.md`（本文件）
- 历史 ADR / 需求书保留当时表述，只追加不改写。

## 6. 红线与约定

- Java 21；Spring Boot 3.5.16；AgentScope Java 2.0.2；单 Maven 模块；不引入 Spring AI。
- 不做 PostgreSQL M1；当前仅 in-memory。
- 不提前创建空包 / 空模块 / 无人引用死代码。
- `web/` 不进入 Maven。
- 公开安全：不提交真实 key、个人邮箱、本机绝对路径、组织信息、构建产物。
- Git：commit 需用户明确指示；push 一律用户手动执行。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。

## 7. 下一步

1. 用户已示意「直接提交」——等待用户最终确认后按建议 commit（见下）；
2. 下次接手先 `mvn clean test` 验证 ADR 13（本会话未跑）；
3. 用户按教程配置 IDEA：Active profiles = `local` 并填真实 key（若尚未做）；
4. 架构演进按 ADR 10 §4 触发器进行：第一个非 chat 业务（memory / persona）立项时同级新建业务包；
5. 下一个功能任务：M1 PostgreSQL `session_message` 持久化（按 `StoredMessage` 字段建模：id/user_id/session_id/role/content/created_at，落 `chat.store.postgres`）。

### 建议 commit message

文件级交织（11/12/13 多处改同一文件），无法在不破坏「每个 commit 可编译」的前提下干净拆分，建议**一次提交**，或按如下两条提交（11 与 12+13 尽量分）：

```text
# 方案 A：一次提交（推荐——文件交织，拆分会破坏中间态可编译性）
refactor(architecture): ChatMessage 职责拆分 + 冗余清理；配置落 yml + id String 化（ADR 11/12/13）

- ADR 11：LLM 默认配置移入 application.yml，gitignored application-local.yml（local profile）；OpenAliceSettings 绑定
- ADR 12：删除 UserId/SessionId 值对象，id 直接 String 字段
- ADR 13：ChatMessage 收敛为 role/content；新增 chat.store.StoredMessage（id/归属/时间戳）；
  ConversationStore.history(sessionId, limit)；删 ConversationTurn/TurnStatus（进程内零消费状态机）
  与 ChatService.chat()/AgentRuntime.chat()/ChatResponse/ChatResult/ChatStreamEvent.sessionId；
  ChatController 双 if-else 收敛 switch；DEFAULT_USER_ID 下沉 ChatService 私有
- 新增 ADR 11/12/13 文档；同步 AGENTS/README/CONTEXT/index/学习导览 v0.8/handoff
- 注：本会话未运行全量测试（用户示意直接提交），下次先 mvn clean test

# 方案 B：两个 commit（11 先，12+13 后；需按文件挑选，部分文件跨轮次需人工确认）
```

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
