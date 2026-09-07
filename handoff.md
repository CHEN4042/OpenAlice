# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :-- | :-- |
| 日期 | 2026-09-07 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1（会说真话 · 底座）：决策已定稿（需求书 v1.5），P1 目标编码待启动 |
| 分支 | `main` |
| 当前架构 | 根目录四模块：`openalice-core / openalice-memory / openalice-agent / openalice-server` |
| Git 状态 | 会话骨架已推送 `origin/main`；需求书 v1.5 / 记忆 v0.4 / ADR 07 等本轮文档改动已本地 commit（待用户 push） |

## 1. 用户最新确认

**2026-09-07（P1 底座五项决策，需求书 v1.5 / ADR 07）**：

1. **砍 Redis**：唯一外部存储 = PostgreSQL（pgvector）；M1 会话消息落 PG，AgentScope 运行态单实例进程内；Redis 预留后置。
2. **真实 LLM 双 provider**：中转站（OpenAI 兼容，优先）+ DeepSeek 官方 API（兜底），key 走 `OPENALICE_*` 环境变量；移除 Ollama 轨。
3. **`/chat` 直接走 SSE 流式**（P1 起）；WebSocket 留给 P3 语音。
4. **单用户**：固定用户、首启引导配置 `persona/` 文件（user.md 等，参考 OpenHanako）；`user_id` 仅存储预留、不作路由键。
5. **Phase 坐标统一 P1–P4**（P1 会说真话 → P2 记得住 → P3 听得到 → P4 看得见/摸得到）；废弃功能优先级 P0/P1/P2 标记。
6. 本轮只改文档、不跑测试；P1 代码方案待下一轮讨论后启动。

**历史（2026-09-06）**：

1. 对四模块架构方向满意。
2. **取消 `service/` 聚合层**。
3. 四个 Maven 模块直接放在仓库根目录。
4. 更新相关文档后，直接开始写最基础版本代码。
5. 用户已确认本轮提交并推送。
6. **协作约定（2026-09-07 起）**：大改动后 Codex 自行本地 commit，push 留给用户手动执行。

## 2. 当前架构

```text
OpenAlice/
├── openalice-core/
├── openalice-memory/
├── openalice-agent/
├── openalice-server/
├── web/
├── docs/
├── pom.xml
├── README.md
├── AGENTS.md
└── handoff.md
```

依赖规则：

```text
openalice-core ← openalice-memory
openalice-core ← openalice-agent
openalice-agent ← openalice-server
openalice-memory ← openalice-server
```

关键约束：

- `agent` 不直接依赖 `memory`，只依赖 `MemoryPort`；
- `openalice-server.composition` 是唯一组合根；
- `openalice-server` 是唯一 Spring Boot executable application；
- `web/` 只是前端占位，不进入 Maven Reactor；
- 不引入 Spring AI / Spring AI Alibaba。

## 3. Phase 1 实现内容

### `openalice-core`

- `MessageRole`
- `UserId` / `SessionId`
- `ChatMessage`
- `MemoryQuery` / `MemoryRecord`
- `MemoryPort`
- `TracePort`（默认空实现，保留扩展点）

### `openalice-memory`

- `InMemoryMemoryPort`
- 按 `(userId, sessionId)` 隔离
- 同一 session 的 append / history 使用同一对象锁，避免并发写入丢消息
- history 返回不可变副本
- 不依赖 Spring / AgentScope

### `openalice-agent`

- `AgentRuntime` / `ChatResult` / `AgentRuntimeFactory`
- `AgentScopeAgentRuntime`
- `DeterministicChatModel`
- `AgentRuntimeProperties`
- `PersonaPrompt`
- 使用 AgentScope `HarnessAgent + RuntimeContext + InMemoryAgentStateStore`
- 不保存会话状态在 runtime 字段中

### `openalice-server`

- `OpenAliceApplication`
- `POST /api/v1/chat`（当前同步 JSON；P1 目标改 SSE 流式）
- `GET /api/v1/users/{userId}/sessions/{sessionId}/messages`
- `GET /actuator/health`
- `CompositionConfiguration`

### P1 底座目标（v1.5 新增，尚未编码）

- 真实双 provider 接入：中转站（优先）+ DeepSeek 官方兜底，`OPENALICE_*` 环境变量，复用 `OPENALICE_LLM_BASE_URL / API_KEY / MODEL`；替换 `DeterministicChatModel`。
- `POST /api/v1/chat` 改 `text/event-stream`（SSE：`text_delta / done / error`）。
- M1：`PgSessionMemoryPort`（表 `session_message`）替换 `InMemoryMemoryPort`，会话重启不丢。
- `persona/` 初始化：首启引导生成 `user.md` 等（单用户）。

## 4. 构建与验证

已在 2026-09-06 完成：

- `mvn clean test`：**通过**
  - core：2 个测试通过
  - memory：4 个测试通过，含同一 session 128 次并发写入
  - agent：2 个测试通过，含 AgentScope 双 session 并发隔离
  - server：2 个测试通过，含 MockMvc 全链路
- `mvn -pl openalice-server -am package`：**通过**
- 真实进程冒烟：**通过**
  - jar 启动于 18081
  - `POST /api/v1/chat` 返回 `{"userId":"user","sessionId":"default","reply":"收到：你好"}`
  - `GET /api/v1/users/user/sessions/default/messages` 返回 user / assistant 两条记录
  - `GET /actuator/health` 返回 `{"status":"UP"}`

常用命令：

```bash
mvn clean test
mvn -pl openalice-server -am package
mvn -pl openalice-server spring-boot:run
```

请求示例：

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user","sessionId":"default","message":"你好"}'
```

## 5. 关键决策与红线

- AgentScope Java 2.0.2；如关键回归回退 2.0.0。
- Spring Boot 3.5.16 仅作 Web 壳。
- Java 21。
- P1 底座起接真实 LLM：中转站（优先）+ DeepSeek 官方兜底；`DeterministicChatModel` 仅过渡。
- P1 底座目标 = M1 会话消息 PG 持久化（`session_message`，重启不丢）；`InMemoryMemoryPort` 仅过渡；**Redis 已砍、预留后置**。
- 单用户：`user_id` 仅存储预留、不作路由键；首启引导 `persona/` 初始化。
- 文本走 SSE 流式；WebSocket 留给 P3 语音。
- 语音 / learning / OpenHanako 式能力只保留架构位置。
- 记忆 M1–M3 自研，AgentScope memory 只做桥接不替代。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
- 参考项目只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。

## 6. 下一步

1. 与用户讨论并启动 **P1 底座编码**（顺序建议）：真实双 provider 接入 → `/chat` SSE 流式（首 token p95 < 2s 验收）→ M1 `session_message` PG 持久化 → `persona/` 首启初始化。
2. 技术验证：AgentScope Java 2.0 流式（event stream）在真实 ChatModel 下是否稳定（锁 2.0.2，回退线 2.0.0）；`PersonaPrompt` 与 `AgentRuntimeProperties.systemPrompt` 的统一。
3. 记忆侧 P2（记得住）再启动：M2 / A1 / 写入管线 / M3（详见《记忆架构设计 v0.4》§8）。

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
