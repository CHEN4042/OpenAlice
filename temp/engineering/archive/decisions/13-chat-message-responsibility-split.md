# ADR 13 · ChatMessage 职责拆分与全项目冗余清理

- 状态：✅ 当前有效
- 日期：2026-09-08
- 关系：在 ADR 12（id String 化）基础上进一步收敛 `ChatMessage`；修订 ADR 09 §2.3（删除 `ConversationTurn` / `TurnStatus` 的进程内状态机）；核心边界与单模块结论不变

## 1. 背景

用户 review 后反馈：`ChatMessage` 仍然「抽象、塞了太多东西」——一个 6 字段 record（id / role / content / userId / sessionId / timestamp）+ 常量 + 工厂 + normalize/require 校验，**一个类型混了四类职责**：

1. LLM 对话语义（role / content）；
2. 会话归属（userId / sessionId）；
3. 存储元数据（id / timestamp）；
4. 产品部署策略（`DEFAULT_USER_ID`）。

按 Jarvis 心智（`ConversationMessage` 实体不带 role、字段直观、包内按类型整理），消息模型应只表达「谁说、说了什么」，其余下沉到存储行。顺带全项目扫描，发现多处同类冗余。

## 2. 决策

### 2.1 消息收敛为纯 LLM 消息

```java
// model/ChatMessage —— 跨 chat/agent 共享，只有真共享物
record ChatMessage(MessageRole role, String content)

// chat/store/StoredMessage —— 存储行：业务消息 + 归属 + 存储元数据
record StoredMessage(String id, String userId, String sessionId, ChatMessage message, Instant createdAt)
```

- `ChatMessage` 只剩 `(role, content)`，配 `user(content)` / `assistant(content)` 工厂与构造校验；
- `StoredMessage` 落在 `chat.store`（存储侧概念不进共享 `model`），`user(...)` / `assistant(...)` 工厂带 `userId / sessionId / content`；
- `MessageRole` 增 `wireValue()`（`"user"` / `"assistant"` / `"system"`），消除 controller / AgentScope 适配器里的 `name().toLowerCase()` 重复与链式 if-else。

### 2.2 ConversationStore 按 session 收口

- `append(StoredMessage)`；`history(String sessionId, int limit)`——接口不再带 `userId` 入参（单用户语义），存储行仍保留 `userId` 供未来认证，未来多用户不推翻 schema。

### 2.3 删除 ConversationTurn / TurnStatus（进程内状态机）

ADR 09 §2.3 曾引入 `ConversationTurn`（10 字段 record + RECEIVED/RUNNING/COMPLETED/FAILED/CANCELLED 状态机）表达 turn 生命周期。实际落地后：

- `ChatService` 里 `currentTurn` 状态**只 set 不读、不落库、不暴露**，流结束即丢弃——运行时零消费者；
- AgentScope 适配器只取 `turn.userId() / turn.sessionId()` 构造 `RuntimeContext`；
- ADR 09 §4.2 自己声明「若未来需要审计，再扩展 `ConversationStore`，不提前加死接口」。

故删除 `ConversationTurn` / `TurnStatus` 两个文件，`AgentRequest` 直接携带 `String userId / sessionId`；将来出现真实审计 / turn 持久化需求时，以 `ConversationStore` 扩展（postgres 阶段）的形式回归，不再放进程内死状态机。

### 2.4 清理无消费者便利层（YAGNI）

- 删 `ChatService.chat()` blocking 便捷方法（HTTP 只走 SSE，仅测试使用）与仅服务它的 `dto/ChatResponse`；
- 删 `AgentRuntime.chat()` default blocking 方法与仅服务它的 `agent/runtime/ChatResult`；
- `ChatController` 双份 if-else（`eventName` + `toStreamEvent`）收敛为单个 switch：SSE event name 直接用 `ChatStreamEvent.type()`；
- `ChatStreamEvent` 移除重复的 `sessionId` 字段（每个 SSE 流已绑定单一 session，事件内重复是冗余）；
- `dto/MessageView` 变纯 record，映射放 `ChatService.history()`，避免 dto 反向依赖 `chat.store`；
- `ChatMessage.DEFAULT_USER_ID` → `ChatService` 私有常量 `DEFAULT_USER_ID`（产品部署策略内聚到唯一编排点，测试用普通字符串不再引用）。

## 3. 权衡

| 方案 | 字段直观 | 单一职责 | 类型安全 |
| :-- | :-- | :-- | :-- |
| 一个消息类型全字段（原状） | ❌ | ❌ 混 4 职责 | ✅ 集中在构造器 |
| ChatMessage + StoredMessage（采用） | ✅ 消息=语义、存储行=行 | ✅ | ✅ 各自构造器校验 |
| Turn 状态机保留（原状） | ❌ 10 字段无消费者 | ❌ | — |
| 删 Turn，AgentRequest 带 userId/sessionId（采用） | ✅ 端口形状最小 | ✅ | ✅ 构造器校验 |

- 取舍理由：**结构清晰与单一职责优先**。id / 归属 / 时间戳这些「存储才有意义」的数据不该出现在纯语义消息上；`ConversationTurn` 这类为未来设计、当下零消费的进程内对象违反「不提前加死接口」原则，先删，等真实需求出现再以持久化形态回归。

## 4. 影响

- `model` 只剩 `ChatMessage` / `MessageRole`（共享词汇最小化）；存储侧 `chat.store` 增加 `StoredMessage`；
- `chat.service` 编排变简单：无 turn 状态机、`DEFAULT_USER_ID` 内聚；
- `agent` 端口 `AgentRequest(userId, sessionId, context, systemPrompt)`，`AgentRuntime` 只留 `stream`；
- dto：删 `ChatResponse`，`ChatStreamEvent` 少一个字段，`MessageView` 纯 record；
- 删除文件：`ConversationTurn` / `TurnStatus` / `ChatResult` / `ChatResponse`；
- 测试同步：删 `ConversationTurnTest`，其余改新 API；controller 集成测试协议断言不变（SSE type/reply、history role/content）；
- M1 PostgreSQL `session_message` 表按 `StoredMessage` 字段建模：`id / user_id / session_id / role / content / created_at`，与存储行一一对应。

## 5. 后续

- 等用户 review 后 commit（本会话 ADR 11 + 12 + 13 一起或分批）；
- 架构演进触发器不变（ADR 10 §4）：第一个非 chat 业务立项时同级新建业务包。
