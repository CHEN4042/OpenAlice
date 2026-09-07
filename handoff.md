# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :-- | :-- |
| 日期 | 2026-09-06 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | Phase 1 implementation |
| 分支 | `main` |
| 当前架构 | 根目录四模块：`openalice-core / openalice-memory / openalice-agent / openalice-server` |
| Git 状态 | Phase 1 初始架构已提交并推送到 `origin/main`，工作区干净 |

## 1. 用户最新确认

1. 对四模块架构方向满意。
2. **取消 `service/` 聚合层**。
3. 四个 Maven 模块直接放在仓库根目录。
4. 更新相关文档后，直接开始写最基础版本代码。
5. 用户已确认本轮提交并推送；后续仍默认不主动 commit / push，除非用户明确指示。

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
- `POST /api/v1/chat`
- `GET /api/v1/users/{userId}/sessions/{sessionId}/messages`
- `GET /actuator/health`
- `CompositionConfiguration`

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
- Phase 1 不接外部 LLM API，使用 deterministic model。
- Phase 1 不做持久化，使用 in-memory memory。
- 语音 / learning / OpenHanako 式能力只保留架构位置。
- 记忆 M1–M3 自研，AgentScope memory 只做桥接不替代。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
- 参考项目只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。

## 6. 下一步

1. 下一步优先二选一：接入真实 LLM model，或先设计持久化 memory / M1。
2. 后续如需继续微调 AgentScope Harness 安全面，可研究内置 `wait_async_results` 工具是否能完全关闭；Phase 1 其他不需要的 Harness 能力已默认关闭。
3. 后续可统一 `PersonaPrompt` 与 `AgentRuntimeProperties.systemPrompt` 的关系；当前二者内容接近，Phase 1 尚未形成行为差异。

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
