# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-09 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | HEAD `3aa198c` = **ADR 11/12/13 已合入**；工作区含本轮「注释中文化 + 移除 mock + Agent 执行层重构（方案 A）」，**编码 + 文档已完成，待 commit**（用户已明确「直接提交，不跑测试」，commit 后用户手动 push） |
| 分支 | `main` |
| 当前架构 | 单 Maven 模块；根包 `com.openalice`；顶层 `model / agent / llm / chat(service\|store) / controller / dto / config`；`runtime/` 与 `agent.llm` 已拆除 |
| Git 状态 | 本轮改动全部**未 commit**（源码 + 测试 + 本文档）；`application-local.yml` 已 gitignore 且含本机真实 key，**绝不提交 / 不 cat / 不打印** |
| 验证 | `mvn -q test-compile` 通过（Java 21）；**全量测试未跑**（用户明确「不需要测试，直接提交」） |

## 1. 本轮内容（未 commit，一次提交）

### 1.1 顶层类注释中文化（用户要求）

所有类的「整个 class 是什么」注释从英文改为中文（JavaDoc 首段中文化）；涉及 `model / chat.store / chat.service / controller / dto / config / agent / llm` 全部源码文件。英文细节注释保留无妨，重点是**类级说明**可读。

### 1.2 移除 mock 链路（用户已接真实 key）

- 删除 `agent/llm/DeterministicChatModel.java`（mock 回退）；
- `pom.xml` 移除 `agentscope-harness` 依赖（ReActAgent 在 `agentscope-core` 内）；
- `application.yml` 移除 `openalice.agent.reply-prefix` / `workspace`，provider 注释去掉 `mock` 选项；
- `LlmModelFactory`：无 key 不再回落 mock，`auto` = AgentRouter(中转) key 优先 → DeepSeek → 都没有直接抛错。

### 1.3 Agent 执行层重构（方案 A，本轮核心）

用户原话（大意）：「agent 包里有点乱——LLM 包里装了什么？runtime 是什么？AgentEvent/AgentRequest 不是 DTO 吗？命名参考 Jarvis / 网上开源项目，特别是 runtime 这块」。

落地（详见 §2 目录树）：

- `agent/runtime/AgentRuntime.java` → **`agent/AgentExecutor.java`**（接口，方法不变 `Flux<AgentEvent> stream(AgentRequest)`，`extends AutoCloseable`）；
- `agent/runtime/AgentScopeAgentRuntime.java` → **`agent/AgentScopeReActAgent.java`**（实现 `AgentExecutor`，基于 AgentScope **ReAct 引擎**：推理 → 调用工具 → 观察结果，框架提供循环，本类只装配 + 事件翻译）；
- 删除 `AgentRuntimeFactory` + `AgentRuntimeProperties`（`runtime/` 目录消失）；
- `agent/llm/` 整体上提为顶层 **`com.openalice/llm/`**（`LlmProvider` / `LlmModelFactory` / `ConfiguredHttpTransport`）+ 新增 **`llm/LlmSettings.java`** record（provider/model/baseUrl/proxy/apiKey，替代原 Properties 的 LLM 部分，llm 包不依赖 Spring）；
- 新增 **`agent/tool/`**：`AgentToolkit`（集中注册入口）+ `CurrentTimeTool`（示例工具 `get_current_time`，打通 ReAct 工具调用；初期只此一个）；
- `config/OpenAliceConfiguration`：组合根直连 `AgentScopeReActAgent` + `LlmSettings`；
- `ChatService` 字段 `AgentRuntime` → `AgentExecutor`；`ContextAssembler` 构造改为 `(ConversationStore, @Value(system-prompt), @Value(context-window-size))`，不再注入 `AgentRuntimeProperties`；
- 生产构造签名：`AgentScopeReActAgent(String agentName, String description, String systemPrompt, Duration timeout, LlmSettings llmSettings)`；测试走包级可见的假 `Model` 构造。

### 1.4 测试同步（未运行）

- 新增 `agent/AgentScopeReActAgentTest.java`（内联假 Model + Duration）；
- 删除 `agent/runtime/AgentRuntimePropertiesFixture` 与 `AgentScopeAgentRuntimeTest`；
- `ContextAssemblerTest` / `ChatServiceTest` 替身 → `RecordingAgentExecutor` / `EchoAgentExecutor`；`ChatControllerTest` 同步新协议断言。

## 2. 当前架构（HEAD + 本轮后）

```text
OpenAlice/
├── pom.xml                      # 单模块 Spring Boot 应用，目标 Java 21
├── src/main/java/com/openalice/
│   ├── OpenAliceApplication.java
│   ├── model/                   # 共享词汇：ChatMessage(role/content) · MessageRole（ADR 13 最小化）
│   ├── agent/                   # 内核：一次 agent 执行 → 事件流（不再分 runtime/llm 子包）
│   │   ├── AgentExecutor.java   # 执行端口：stream(AgentRequest) → Flux<AgentEvent>（取代 AgentRuntime）
│   │   ├── AgentScopeReActAgent.java  # 默认实现：AgentScope ReAct 引擎（推理→工具→观察）
│   │   ├── AgentRequest.java    # 显式入参：userId/sessionId + 最近上下文 + system prompt
│   │   ├── AgentEvent.java      # sealed 事件：TextDeltaEvent / DoneEvent / ErrorEvent
│   │   └── tool/                # AgentToolkit（集中注册）· CurrentTimeTool 示例工具
│   ├── llm/                     # 模型接入（顶层，不依赖 Spring）：LlmProvider · LlmModelFactory · LlmSettings · ConfiguredHttpTransport
│   ├── chat/                    # ★ 业务：对话（ADR 10 业务包）
│   │   ├── service/             # ChatService(DEFAULT_USER_ID) · ContextAssembler · SessionCoordinator
│   │   └── store/               # ConversationStore · StoredMessage · memory/InMemoryConversationStore
│   ├── controller/              # ChatController · HealthController · ApiExceptionHandler
│   ├── dto/                     # ChatRequest(sessionId+message) · ChatStreamEvent · MessageView
│   └── config/                  # 组合根：OpenAliceConfiguration + OpenAliceSettings（绑定 openalice.*）
├── src/test/java/com/openalice/  # 镜像 main：AgentScopeReActAgentTest · ChatServiceTest · ContextAssemblerTest · SessionCoordinatorTest · InMemoryConversationStoreTest · ChatControllerTest · ChatMessageTest
└── resources/application.yml    # 公共安全默认值；本机 key 在 gitignored application-local.yml
```

职责链（一次 `/api/v1/chat`）：

```text
ChatController          # HTTP/SSE 翻译，不写业务
  → ChatService        # 单 turn 编排 + USER/ASSISTANT 持久化（私有 DEFAULT_USER_ID）
    → SessionCoordinator  # 同 session 串行，不同 session 并行（信号量覆盖整条流式生命周期）
    → ContextAssembler   # 从 ConversationStore 拉最近 N 条 → 组装 AgentRequest
    → AgentExecutor      # = AgentScopeReActAgent：clearContext 后显式上下文驱动 ReAct，产出 Flux<AgentEvent>
```

依赖规则：`model ← chat.store`、`model ← agent`、`agent → model + llm + agent.tool`、`chat.service → model + chat.store + agent`、`controller → chat.service + dto`、`config` 组装 chat.store + llm + agent；llm 不依赖 com.openalice 内任何包。

## 3. 文档状态（本轮已同步）

- `AGENTS.md`：目录树 agent/llm 块、包依赖单向、职责链（AgentExecutor）、测试分层措辞；
- `README.md`：目录树 + 职责链（AgentRuntime → AgentExecutor）；
- `docs/CONTEXT.md`：顶层包分层表述、auto 回退链（无 mock）、`com.openalice.llm.LlmModelFactory` 路径；
- `docs/代码学习导览 v0.1.md`：表头升 v0.8 + §0.1 红字提示「§2~§7 结构/命名章节滞后，正文仍写旧 runtime/llm 结构，待下次完整重写」；
- `handoff.md`（本文件）。
- **未新增 ADR**：本轮属结构整理，按仓库纪律应在下轮补 **ADR 14「Agent 执行层命名与结构整理」**（记录 AgentExecutor / AgentScopeReActAgent / llm 顶包 / tool/ / mock 移除），并同步 docs/index.md 变更记录。

## 4. 红线与约定

- Java 21（本机默认 shell 是 Java 17，编译需先 `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`）。
- Spring Boot 3.5.16；AgentScope Java 2.0.2（core + extensions-model-openai，无 harness）；单 Maven 模块；不引入 Spring AI。
- 不提前创建空包 / 空模块 / 无人引用死代码；`web/` 不进入 Maven。
- 公开安全：不提交真实 key、个人邮箱、本机绝对路径、组织信息、构建产物；`application-local.yml` 含真实 key，只读不改不提交。
- Git：commit 需用户明确指示；push 一律用户手动执行。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。

## 5. 下一步

1. **commit 本轮全部改动**（用户已同意直接提交，不跑测试；建议 message 见下），然后**用户手动 push**；
2. 下轮接手先 `mvn clean test`（本会话只过了 test-compile）；
3. 补 ADR 14 + `docs/index.md` 变更记录 + 完整重写《代码学习导览》§2~§7；
4. 架构演进按 ADR 10 §4 触发器进行；下一个功能任务仍为 M1 PostgreSQL `session_message` 持久化（按 `StoredMessage` 建模：id/user_id/session_id/role/content/created_at，落 `chat.store.postgres`）；
5. 可讨论项：AgentScope 官方同 (user,session) 串行语义 vs 自研 `SessionCoordinator` 是否有重叠（暂未深究）。

### 建议 commit message

```text
refactor(agent): Agent 执行层整理——AgentExecutor + AgentScopeReActAgent + 示例工具

- 中文注释：全部类级说明统一中文化
- 移除 mock 链路：删 DeterministicChatModel、agentscope-harness 依赖、reply-prefix/workspace 配置
- runtime/ 拆除：AgentRuntime → agent.AgentExecutor；AgentScopeAgentRuntime → AgentScopeReActAgent（ReAct 引擎）
- agent/llm 上提为顶层 llm/：LlmProvider · LlmModelFactory · LlmSettings · ConfiguredHttpTransport
- 新增 agent/tool/：AgentToolkit 集中注册 + CurrentTimeTool 示例，打通 ReAct 工具调用
- 组合根直连 AgentScopeReActAgent；ChatService/ContextAssembler 改注入 AgentExecutor
- 测试同步：AgentScopeReActAgentTest（假 Model），service/controller 替身改 AgentExecutor
- 注：仅 mvn -q test-compile 通过，未跑全量测试（用户要求直接提交）
```

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
