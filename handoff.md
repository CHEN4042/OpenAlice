# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :-- | :-- |
| 日期 | 2026-09-07 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1（会说真话 · 底座）：真实双 provider 已接入（`7f1b4dd`，本地）；**四模块 → 单模块收敛重构完成（未 commit）**；SSE / M1-PG / persona 待编码 |
| 分支 | `main`（本地领先 origin/main 1 commit；push 由用户手动执行） |
| 当前架构 | **单 Maven 模块**：根 `pom.xml` 即 Spring Boot 应用（`io.openalice:openalice`），根包 `openalice`，顶层包分层 `model / port / memory / agent(runtime|llm) / service / controller / dto / config`（ADR 08，修订 ADR 06） |
| Git 状态 | 双 provider（`7f1b4dd`）已本地 commit、待用户 push；单模块重构代码 + 文档同步**已完成、未 commit**，待用户 review 后本地 commit |

## 1. 用户最新确认

**2026-09-07（单模块收敛重构）**：

1. 用户改主意：放弃四 Maven 模块，**收敛为单模块**，大致仿照本地 Jarvis（单 Spring Boot 模块 + 顶层分包），包命名采用 `model`（不用 domain/entity）。
2. 改造过程多参考开源项目（openhanako / nanobot / openclaw / khoj / 本地 Jarvis），取其精华去其糟粕。
3. 重构完成、测试全绿后，先展示给用户 review，**不要直接 commit**（AGENTS.md Git 红线：commit 需用户明确指示）。
4. 已落 **ADR 08**（`docs/decisions/08-single-module-convergence.md`）：单模块收敛决议，修订 ADR 06 顶层布局。

**2026-09-07（P1 底座五项决策，需求书 v1.5 / ADR 07）**：

1. **砍 Redis**：唯一外部存储 = PostgreSQL（pgvector）；M1 会话消息落 PG，AgentScope 运行态单实例进程内；Redis 预留后置。
2. **真实 LLM 双 provider**：中转站（OpenAI 兼容，优先）+ DeepSeek 官方 API（兜底），key 走 `OPENALICE_*` 环境变量；移除 Ollama 轨。
3. **`/chat` 直接走 SSE 流式**（P1 起）；WebSocket 留给 P3 语音。
4. **单用户**：固定用户、首启引导配置 `persona/` 文件（user.md 等，参考 OpenHanako）；`user_id` 仅存储预留、不作路由键。
5. **Phase 坐标统一 P1–P4**（P1 会说真话 → P2 记得住 → P3 听得到 → P4 看得见/摸得到）；废弃功能优先级 P0/P1/P2 标记。

**2026-09-07（代码学习导览）**：新增《代码学习导览 v0.1》（`docs/`，现内容 v0.2），用于帮唯一开发者读懂 AI 写的代码；约定三条学习纪律——① AI 讲解按 What/Why/对应文件；② 用户需回讲验证理解；③ 用户需亲手改一处并跑通。该文档**随代码维护**（接口/端点/模型变更必更新，见其 §10）。

**历史（2026-09-06）**：

1. 曾对四模块架构方向满意 → **2026-09-07 已被单模块收敛替代（ADR 08）**。
2. **协作约定（2026-09-07 起）**：大改动后 Codex 自行本地 commit，push 留给用户手动执行；commit 前先给用户 review。

## 2. 当前架构（单模块 · ADR 08）

```text
OpenAlice/
├── pom.xml                      # 单模块 Spring Boot 应用（io.openalice:openalice），唯一可运行 jar
├── src/main/java/openalice/
│   ├── OpenAliceApplication.java   # 启动类（组合根 = Spring 容器）
│   ├── model/                      # 纯 POJO / 值对象：ChatMessage · MessageRole · UserId · SessionId
│   ├── port/                       # 端口接口：MemoryPort
│   ├── memory/                     # MemoryPort 适配器：InMemoryMemoryPort（将来 PostgreSQL 实现）
│   ├── agent/
│   │   ├── runtime/                # AgentRuntime · AgentScopeAgentRuntime · AgentRuntimeFactory · ChatResult · AgentRuntimeProperties
│   │   └── llm/                    # LlmProvider · LlmModelFactory · DeterministicChatModel · ConfiguredHttpTransport
│   ├── service/                    # ★ ChatService：一次 /chat 的业务编排（见下）
│   ├── controller/                 # ChatApiController · HealthController · ApiExceptionHandler
│   ├── dto/                        # ChatRequest · ChatReply · MessageView
│   └── config/                     # OpenAliceConfiguration：Spring @Configuration 组合根
├── src/main/resources/application.yml   # server.port=8080
├── src/test/java/openalice/        # 测试镜像 main 的包结构
├── web/                            # 前端占位，不进入 Maven
├── docs/                           # 索引 / 需求书 / CONTEXT / 学习导览 / decisions（handoff 在根目录）
├── README.md / AGENTS.md
└── handoff.md
```

包依赖纪律（防乱，靠 AGENTS.md + 评审维护，暂无 ArchUnit）：

```text
model ← port ← memory
model ← agent（runtime / llm）
service → port + agent + model        # ChatService 编排：存消息 → 问 agent → 存回复
controller → service
config → memory + agent + model       # Spring 装配组合根
```

关键约束：

- `agent` 不依赖 `memory`，且 runtime 现在**连 `MemoryPort` 都不持有**——只做「校验 → 问一次模型 → 返回回复」；
- 编排收敛到 `service.ChatService`；`controller` 只接 HTTP，依赖 `ChatService` 门面（学 Jarvis）；
- `config.OpenAliceConfiguration` 是唯一手写装配点（换存储只改它的 `memoryPort()` 一个方法）；
- `web/` 只是前端占位，不进入 Maven Reactor；
- 不引入 Spring AI / Spring AI Alibaba。

## 3. Phase 1 实现内容（按包）

### `model/`（原 core.domain）
- `MessageRole` / `UserId` / `SessionId` / `ChatMessage`（纯 POJO，零框架依赖）

### `port/`（原 core.port）
- `MemoryPort`（append / history 抽象）

### `memory/`（原 openalice-memory）
- `InMemoryMemoryPort`：按 `(userId, sessionId)` 隔离；同 session append/history 共用对象锁防并发丢消息；history 返回不可变副本；不依赖 Spring / AgentScope

### `agent/runtime/` + `agent/llm/`（原 openalice-agent）
- `AgentRuntime` / `ChatResult` / `AgentRuntimeFactory` / `AgentScopeAgentRuntime` / `AgentRuntimeProperties`
- `LlmProvider` / `LlmModelFactory` / `DeterministicChatModel`（mock）/ `ConfiguredHttpTransport`
- 使用 AgentScope `HarnessAgent + RuntimeContext + InMemoryAgentStateStore`；不把会话状态存在 runtime 字段

### `service/`（本次新增）
- `ChatService`：一次 `/chat` 编排——先存 user 消息 → 问 agent → 再存 assistant 回复

### `controller/` + `dto/`（原 openalice-server.api）
- `POST /api/v1/chat`（当前同步 JSON；P1 目标改 SSE 流式）
- `GET /api/v1/users/{userId}/sessions/{sessionId}/messages`
- `GET /actuator/health`
- `ApiExceptionHandler`

### P1 底座目标（需求书 v1.5）

- ✅ 真实双 provider 接入（已编码，commit `7f1b4dd`，待用户 push）：中转站（优先）+ DeepSeek 官方兜底，`OPENALICE_*` 环境变量，auto 回退链 = 中转 → DeepSeek → 无 key 回落 mock；`DeterministicChatModel` 保留为回落。
- ⏳ `POST /api/v1/chat` 改 `text/event-stream`（SSE：`text_delta / done / error`）。
- M1：`PgSessionMemoryPort`（表 `session_message`）替换 `InMemoryMemoryPort`，会话重启不丢。
- `persona/` 初始化：首启引导生成 `user.md` 等（单用户）。

## 4. 构建与验证

**单模块重构后（本次）**：

- `mvn clean test`：**通过（13 tests）**
  - model 2 / memory 4（含同 session 128 次并发写入）/ agent 2（AgentScope 双 session 并发隔离）/ service 3（ChatServiceTest：fake runtime + InMemoryMemoryPort 测编排顺序）/ controller 2（MockMvc 全链路）

**历史（四模块时代，2026-09-06）**：

- `mvn clean test` 通过（core 2 / memory 4 / agent 2 / server 2）；真实进程冒烟通过（jar 启动、`POST /chat` 返回「收到：你好」、消息历史 / health 正常）。冒烟细节见 git 历史，重构后未重跑进程冒烟。

常用命令（单模块，无 `-pl`）：

```bash
mvn clean test
mvn package
mvn spring-boot:run          # 默认 8080
```

请求示例：

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user","sessionId":"default","message":"你好"}'
```

## 5. 关键决策与红线

- AgentScope Java 2.0.2；如关键回归回退 2.0.0。
- Spring Boot 3.5.16 仅作 Web 壳；Java 21；**单 Maven 模块**（ADR 08 修订 ADR 06）。
- 架构参照：本地 Jarvis（单模块 + 顶层分包 + controller/service 分层）、openhanako / nanobot / openclaw / khoj（仅思想参考，语言无关）。
- P1 底座起接真实 LLM：中转站（优先）+ DeepSeek 官方兜底；`DeterministicChatModel` 仅回落。
- P1 底座目标 = M1 会话消息 PG 持久化（`session_message`，重启不丢）；`InMemoryMemoryPort` 仅过渡；**Redis 已砍、预留后置**。
- 单用户：`user_id` 仅存储预留、不作路由键；首启引导 `persona/` 初始化。
- 文本走 SSE 流式；WebSocket 留给 P3 语音。
- 语音 / learning / OpenHanako 式能力只保留架构位置。
- 记忆 M1–M3 自研，AgentScope memory 只做桥接不替代。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
- 参考项目只作参考，不约束最终实现。
- 仓库按 public 安全标准维护：不出现公司/组织信息、真实 key、个人邮箱、本机路径、SSH 细节。
- Git：大改动后 Codex 本地 commit（先 review），**push 一律用户手动**。

## 6. 下一步

1. **单模块重构收尾**（当前）：待用户 review 代码 + 文档（ADR 08 / AGENTS / README / CONTEXT / index / 学习导览 v0.2 / handoff）→ 确认后本地 commit（建议 message 见下），push 由用户执行。
2. 与用户讨论并启动 **P1 底座剩余编码**（顺序建议）：`/chat` SSE 流式（首 token p95 < 2s 验收）→ M1 `session_message` PG 持久化 → `persona/` 首启初始化。
3. 双 provider（`7f1b4dd`）待用户 push 后真机验证：auto 回退、中转 WAF 指纹头、代理组合。
4. 技术验证：AgentScope Java 2.0 流式（event stream）在真实 ChatModel 下是否稳定（锁 2.0.2，回退线 2.0.0）；`PersonaPrompt` 已随重构删除（0 引用死代码，未来按 persona 需求重建）。
5. 记忆侧 P2（记得住）再启动：M2 / A1 / 写入管线 / M3（详见《记忆架构设计 v0.4》§8）。
6. **文档纪律**：每次代码/接口/端点大改动后，同步维护《代码学习导览》（§4 链路 / §5 专题 / §8 追踪表），并引导用户回讲 + 亲手改一处。

### 建议 commit message（单模块重构）

```text
refactor(openalice): 四模块收敛为单模块 + service 编排层（ADR 08）

- Maven 四模块（core/memory/agent/server）收敛为单模块 openalice，根 pom 即应用
- 顶层包分层 model/port/memory/agent(runtime|llm)/service/controller/dto/config
- 新增 service.ChatService：一次 /chat 编排（存消息 → 问 agent → 存回复）
- agent runtime 瘦身：只答一次、不再持有 MemoryPort；controller 只依赖 ChatService
- 删 0 引用死代码：PersonaPrompt / MemoryQuery / MemoryRecord / TracePort
- 文档同步：ADR 08 / AGENTS / README / CONTEXT / index / 代码学习导览 v0.2 / handoff
- 测试全绿：13 tests（model 2 / memory 4 / agent 2 / service 3 / controller 2）
```

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
