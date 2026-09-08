# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-08 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1.5（语义重构）已提交；**ADR 10 业务包结构**（本会话）编码与文档同步完成，待用户 review |
| 分支 | `main` |
| HEAD | `276c9e5 refactor(architecture): 完成 P1.5 语义与包结构整理`（P1.5 已 commit） |
| 当前架构 | 单 Maven 模块；根包 `com.openalice`；顶层 `model / agent(runtime\|llm) / chat(service\|store) / controller / dto / config`（ADR 10，修订 ADR 09 顶层布局） |
| Git 状态 | 工作区含 ADR 10 迁移与文档改动，**未 commit**；`git mv` 已保留 rename 配对，review 确认后 `git add -A` 即可 |
| 验证 | 2026-09-08 `mvn clean test`（本机 **Java 21.0.8**）全绿：22 tests / 0 failures / 0 errors |

> 注：旧 handoff 写「P1.5 未 commit」「本机只有 Java 17」已过时——P1.5 在 `276c9e5` 已提交；当前环境为 Java 21，可跑标准测试，无需 release 覆盖与 Byte Buddy agent 参数。

## 1. 用户最新确认（ADR 10）

1. 包结构偏好确认：不是「类型优先 vs 业务优先」二选一，而是**两级组织**——顶层按业务/内核/共享分，业务包内再按类型整理（喜欢本地 Jarvis `pet/enums` 的形态）。
2. 反对全局 `enums/` 大杂烩：默认留在本地，**被逼无奈（跨业务共享）才上提**。
3. 允许直接改代码：起草 ADR 10 并同步落地物理结构。
4. Git 红线不变：不 commit、不 push；等待用户 review 与明确指示。

## 2. 当前架构（ADR 10 落地）

```text
OpenAlice/
├── pom.xml                      # 单模块 Spring Boot 应用，目标 Java 21
├── src/main/java/com/openalice/
│   ├── OpenAliceApplication.java
│   ├── model/                   # 共享词汇（跨 chat / agent 上提）：ChatMessage · UserId · SessionId · MessageRole · ConversationTurn · TurnStatus
│   ├── agent/                   # 内核：AI 调用
│   │   ├── AgentRequest.java · AgentEvent.java · TextDeltaEvent · DoneEvent · ErrorEvent
│   │   ├── runtime/             # AgentRuntime(端口) · AgentScopeAgentRuntime(适配器) · Factory · Properties · ChatResult
│   │   └── llm/                 # LlmProvider · LlmModelFactory · DeterministicChatModel · ConfiguredHttpTransport
│   ├── chat/                    # ★ 业务：对话（ADR 10 业务包，内部按类型整理）
│   │   ├── service/             # ChatService · ContextAssembler · SessionCoordinator
│   │   └── store/               # ConversationStore · memory/InMemoryConversationStore
│   ├── controller/              # HTTP/SSE 翻译
│   ├── dto/                     # API 出入参
│   └── config/                  # 组合根：OpenAliceConfiguration
├── src/test/java/com/openalice/  # 测试镜像 main 包结构
├── src/main/resources/application.yml
├── docs/
├── web/
└── handoff.md
```

依赖方向：

```text
model ← chat.store
model ← agent
chat.service → model + chat.store + agent
controller → chat.service
config 组装 chat.store + agent.runtime
```

核心边界（语义与 ADR 09 一致）：

- `model`：纯 POJO / 值对象，无框架与持久层依赖；被 chat 业务与 agent 内核共用，故上提共享；
- `chat.service`：Turn 生命周期、上下文组装、持久化与 session 并发控制；`chat.store`：会话历史唯一真相源（端口 + 内存实现）；
- `agent`：不读取 `ConversationStore`，只消费 `AgentRequest`、产出 `AgentEvent`；
- `controller`：只做 HTTP / SSE 翻译；`config`：唯一组合根；
- 未来 `memory` / `persona` 等新业务按 ADR 10 §4 同级新建业务包（内部同样 service/store 组织），不提前建空包。

## 3. 本轮改动（ADR 10）

### 3.1 物理结构

- `src/main/java/com/openalice/service/` → `chat/service/`（ChatService · ContextAssembler · SessionCoordinator）；
- `src/main/java/com/openalice/repository/` → `chat/store/`（ConversationStore · memory/InMemoryConversationStore）；
- `model/`、`agent/`、`controller/`、`dto/`、`config/` 位置不变，但职责重新定性：`model` = 共享词汇（上提区）；`agent` = 内核；`controller/dto` = 入站适配；
- 测试镜像同步：`src/test/java/com/openalice/{service,repository}` → `chat/{service,store}`；
- 纯物理移动 + import 改写，**无行为 / API / 端点变更**。

### 3.2 新增决策文档

- `docs/decisions/10-package-layout-evolution-rules.md`：4 条包结构规则（判归属 / 顶层按角色 / 包内按类型 / 业务间不串门）+ 演进触发器（第一个非 chat 业务包、第二个入站协议、顶层目录 ~10 个时才动结构）。

## 4. 测试状态

2026-09-08（Java 21.0.8）验证结果：

```text
22 tests passed
0 failures
0 errors
```

测试分布（包路径随迁移更新）：

```text
model: 4            （ChatMessageTest 2 + ConversationTurnTest 2）
chat.store.memory: 4
chat.service: 8      （ChatServiceTest 5 + ContextAssemblerTest 1 + SessionCoordinatorTest 2）
agent.runtime: 3
controller: 3
```

## 5. 文档状态

本会话已同步：

- `docs/decisions/10-package-layout-evolution-rules.md`（新增）
- `AGENTS.md`（目录树、依赖方向、ADR 权威层级、开发规范）
- `docs/index.md`（ADR 10 行、ADR 09 状态修订、整理记录）
- `docs/CONTEXT.md`（顶层包分层、ContextAssembler / ConversationStore 路径）
- `README.md`（架构树、文档导航）
- `docs/代码学习导览 v0.1.md`（升 v0.6：目录树、依赖、文件路径、测试地图、变更记录）
- `handoff.md`（本文件）

旧历史 ADR / 需求书保留当时的 `repository`、`service`、旧包路径表述，这是历史记录，不需要改写。

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

1. 用户 review 当前 diff（ADR 10 物理迁移 + 文档）；
2. review 确认后执行 `git add -A`（`git mv` 已保留 rename 配对），再按用户指示本地 commit（建议 message 见下）；
3. 架构演进按 ADR 10 §4 触发器进行：第一个非 chat 业务（memory / persona）立项时，同级新建业务包并保持内部 service/store 组织；
4. 下一个功能任务：M1 PostgreSQL `session_message` 持久化（落 `chat.store.postgres`）；
5. M1 后做 persona 首启初始化。

### 建议 commit message

```text
refactor(architecture): 落地 ADR 10 业务包自治结构

- 新增 ADR 10：业务包自治 + 包内按类型整理 + 共享才上提 + 演进触发规则
- repository/ → chat/store/，service/ → chat/service/，测试镜像同步
- model 定位为跨 chat/agent 共享词汇；agent 定位为内核
- 纯物理移动 + import 改写，无行为/API 变更
- 同步 AGENTS / README / CONTEXT / docs/index / 学习导览 v0.6 / handoff
- mvn clean test（Java 21）：22 tests 全绿
```

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
