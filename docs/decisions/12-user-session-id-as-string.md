# ADR 12 · UserId / SessionId 值对象降为 String 字段（Jarvis 直观风格）

- 状态：✅ 当前有效
- 日期：2026-09-08
- 关系：修订 ADR 09 / ADR 10 中「`UserId` / `SessionId` 为独立值对象」的 `model` 设计细节；不改单用户 API 语义，也不改「存储保留 userId 供未来认证」的结论

## 1. 背景

ADR 09 曾把 `UserId` / `SessionId` 建模为独立值对象并置于 `model`，理由是类型安全 + 校验集中。落地后用户反馈（参照 Jarvis 心智模型）两点：

1. **结构不直观**：在 `ChatMessage` / `ConversationTurn` 上看到的是两个不认识的类型，得点进 `model` 才知道它们是「用户 id / 会话 id」——不如直接看 `String userId / sessionId` 一眼就懂；
2. 实际使用面窄：单用户 API 下，`UserId` 只有常量 `DEFAULT` 一个取值，值对象没有发挥类型安全的用武之地，反而增加了「打开文件才知道含义」的认知成本。

## 2. 决策

### 2.1 id 直接落为 record 的 String 字段

- 删除 `model/UserId.java`、`model/SessionId.java` 两个文件；
- `ChatMessage` / `ConversationTurn` 的字段直接写作 `String userId`、`String sessionId`，字段名即语义，消息上就能读明白「谁发给谁、属于哪段会话」；
- `ConversationStore.history`、`SessionCoordinator.serialize`、`ChatResult` 等端口/返回值同步改为裸 String。

### 2.2 默认用户收敛为常量

- `UserId.DEFAULT` → `ChatMessage.DEFAULT_USER_ID`（值 `openalice-user`），放在消息上、离使用者最近；Controller 仍不接收 `userId`，服务端固定使用该常量。

### 2.3 校验内聚到消息 / Turn 构造器

- 原先由 `UserId.of` / `SessionId.of` 做的非空 / trim 校验，改为在 `ChatMessage` / `ConversationTurn` 的 compact constructor 里对 `userId` / `sessionId` 统一做 `requireNotBlank`——无效值在消息诞生的那一刻就被拒绝，测试断言不变。

### 2.4 存储与并发 key

- `InMemoryConversationStore` 的 `SessionKey(userId, sessionId)` 变为两个 String 的组合 key；`SessionCoordinator` 的并发 map 直接用 `String sessionId` 做 key。行为不变。

## 3. 权衡

| 方案 | 直观性 | 类型安全 | 校验位置 |
| :-- | :-- | :-- | :-- |
| 独立值对象（原状） | ❌ 需进 `model` 才懂 | ✅ | `UserId.of` / `SessionId.of` |
| record 上裸 String（采用） | ✅ 字段名即语义（Jarvis 风格） | ⚠️ 靠构造器集中校验兜底 | `ChatMessage` / `ConversationTurn` 构造器 |

- 取舍理由：**结构清晰优先于类型安全**。id 校验发生在构造器，非法字符串无法进入消息/Turn/存储，类型安全损失可控；将来若出现「多用户 + 跨服务身份传播」等真正需要强类型的场景，再在端口边界恢复值对象即可（演进触发器：出现第二个 userId 来源）。

## 4. 影响

- `model` 包文件减少为 `ChatMessage / ConversationTurn / MessageRole / TurnStatus` 四个；
- 测试全部改字符串字面量 / `ChatMessage.DEFAULT_USER_ID`；`shouldRejectBlankUserId` 改为对 `ChatMessage.user(" ", ...)` 断言抛异常；
- 文档同步：AGENTS.md / README / CONTEXT / 代码学习导览 / handoff；
- M1 PostgreSQL `session_message` 表的 `user_id` / `session_id` 列不变，字段名与库表一一对应，反而更贴近存储。
