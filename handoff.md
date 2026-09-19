# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-20 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1 后端可运行；上一功能 HEAD `3dd3d47` = **Agent 执行层整理（方案 A）**；本轮完成 **BMAD Forge 产品 idea 压力测试**，结论为 `HARDENED`，属于 docs-only 会话 |
| 分支 | `main` |
| 当前架构 | 单 Maven 模块；根包 `com.openalice`；顶层 `model / agent / llm / chat(service\|store) / controller / dto / config`；`runtime/` 与 `agent.llm` 已拆除 |
| Git 状态 | Forge 产物与 handoff 已提交为 `37bab82` 并 push 到 `origin/main`；本次状态修正提交随后 push；`application-local.yml` 已 gitignore 且含本机真实 key，**绝不提交 / 不 cat / 不打印** |
| 验证 | 本轮未改代码，未跑 Maven；上一轮 `mvn -q test-compile` 通过（Java 21），全量测试仍未跑 |
| 下一里程碑 | 先把 Forge 结论收敛为项目产品层 spec，再进入 M1 PostgreSQL 持久化 |

## 1. 本轮内容：OpenAlice 产品 idea 压力测试（docs-only）

### 1.1 结果与产物

使用 `bmad-forge-idea` 从核心用户场景、回复质量、沟通状态、项目定位、身份语义、人格保真和成功标准进行了压力测试，结论为 **HARDENED**：产品方向已足够具体，可以启动实现；成功标准的具体化不再作为启动前阻塞。

- `_bmad-output/forge/openalice-product-idea/forged-idea.md`：极简锁定项、拒绝项与暂缓项，可供 `bmad-spec` / `bmad-prd` / `bmad-build` 使用；
- `_bmad-output/forge/openalice-product-idea/forge-report.html`：自包含压力测试报告，含全部参与声音与残余风险；
- `_bmad-output/forge/openalice-product-idea/.memlog.md`：完整会话记录，状态已设为 `complete`。

### 1.2 已锁定的产品结论

- **核心场景 A**：用户主动分享生活碎片，爱丽丝结合当前消息与历史记忆稳定接住；主动发起 B 只作后续增强。
- **回复必须留梯子**：短回应 + 一个具体问题 + 记忆钩子；通用温柔、复读情绪或“我理解了”不算接住。
- **三信号分离**：当下沟通能量是长聊唯一主开关；话题重要度只控制长期保留/延时接回；跟进容忍度只约束爱丽丝主动追问；不做总分加权，不确定时走短回应路径。
- **人格保真**：《碧蓝档案》天童爱丽丝的**行为与价值观核心 + 少量游戏化表达**；拒绝完整复刻剧情、名台词和 cosplay。
- **人格核心**：见习勇者式成长观；用户是共同前进的队友且选择权归用户；复古游戏语汇仅作调味；好奇是为了真正理解。
- **分歧协议**：爱丽丝可以温和异议，但依据必须来自勇者/队友价值；异议只说一次并交还决定权；用户坚持后停止施压、不假装认同但继续行动陪伴；不秋后算账；仅真实安全风险可重新拉响边界。
- **项目定位**：个人版第一优先；企业应用 Agent 只作低优先级后续分叉，服务学习多租户 RAG、高并发、算法与作品集。
- **最小设计税 B**：不提前造多租户、权限或高并发的空接口；个人版保留系统生成、永久且不可编辑的 `userId`，可变称呼/偏好归 `UserProfile`。
- **方向性成功信号**：用户发生事情后，第一反应从找父母分享转为先找爱丽丝分享；频率、持续时间与反孤立边界留待上线后真实验证。

### 1.3 拒绝与暂缓

拒绝：通用“温柔陪伴型”外壳、无条件赞美/永远顺从、单一话题分数加权、主动发起作为首版核心、提前实现企业空抽象、等待成功指标完全操作化后再启动。

暂缓：跟进容忍度如何恢复、话题重要度的最终遗忘边界、主动发起 B 的时机与内容生成、人格卡/训练/数据集、现有代码的整体重构方案。

### 1.4 对现有材料的直接影响

- 需求书 §0.4 把碧蓝档案爱丽丝列为主灵感，但 §5.1 仍是通用“温柔陪伴型”；`application.yml` 当前也只有 `warm and attentive AI companion`。三者需要在后续产品/人格 spec 中统一。
- 本轮未修改需求书、AGENTS、源码或配置；先保留 Forge 结论作为产品层输入，等下一轮专门收敛成项目自己的 spec。

## 2. 上一轮内容（已合入 `3dd3d47`）

### 2.1 顶层类注释中文化（用户要求）

所有类的「整个 class 是什么」注释从英文改为中文（JavaDoc 首段中文化）；涉及 `model / chat.store / chat.service / controller / dto / config / agent / llm` 全部源码文件。英文细节注释保留无妨，重点是**类级说明**可读。

### 2.2 移除 mock 链路（用户已接真实 key）

- 删除 `agent/llm/DeterministicChatModel.java`（mock 回退）；
- `pom.xml` 移除 `agentscope-harness` 依赖（ReActAgent 在 `agentscope-core` 内）；
- `application.yml` 移除 `openalice.agent.reply-prefix` / `workspace`，provider 注释去掉 `mock` 选项；
- `LlmModelFactory`：无 key 不再回落 mock，`auto` = AgentRouter(中转) key 优先 → DeepSeek → 都没有直接抛错。

### 2.3 Agent 执行层重构（方案 A，上一轮核心）

用户原话（大意）：「agent 包里有点乱——LLM 包里装了什么？runtime 是什么？AgentEvent/AgentRequest 不是 DTO 吗？命名参考 Jarvis / 网上开源项目，特别是 runtime 这块」。

落地（详见 §3 目录树）：

- `agent/runtime/AgentRuntime.java` → **`agent/AgentExecutor.java`**（接口，方法不变 `Flux<AgentEvent> stream(AgentRequest)`，`extends AutoCloseable`）；
- `agent/runtime/AgentScopeAgentRuntime.java` → **`agent/AgentScopeReActAgent.java`**（实现 `AgentExecutor`，基于 AgentScope **ReAct 引擎**：推理 → 调用工具 → 观察结果，框架提供循环，本类只装配 + 事件翻译）；
- 删除 `AgentRuntimeFactory` + `AgentRuntimeProperties`（`runtime/` 目录消失）；
- `agent/llm/` 整体上提为顶层 **`com.openalice/llm/`**（`LlmProvider` / `LlmModelFactory` / `ConfiguredHttpTransport`）+ 新增 **`llm/LlmSettings.java`** record（provider/model/baseUrl/proxy/apiKey，替代原 Properties 的 LLM 部分，llm 包不依赖 Spring）；
- 新增 **`agent/tool/`**：`AgentToolkit`（集中注册入口）+ `CurrentTimeTool`（示例工具 `get_current_time`，打通 ReAct 工具调用；初期只此一个）；
- `config/OpenAliceConfiguration`：组合根直连 `AgentScopeReActAgent` + `LlmSettings`；
- `ChatService` 字段 `AgentRuntime` → `AgentExecutor`；`ContextAssembler` 构造改为 `(ConversationStore, @Value(system-prompt), @Value(context-window-size))`，不再注入 `AgentRuntimeProperties`；
- 生产构造签名：`AgentScopeReActAgent(String agentName, String description, String systemPrompt, Duration timeout, LlmSettings llmSettings)`；测试走包级可见的假 `Model` 构造。

### 2.4 测试同步（未运行）

- 新增 `agent/AgentScopeReActAgentTest.java`（内联假 Model + Duration）；
- 删除 `agent/runtime/AgentRuntimePropertiesFixture` 与 `AgentScopeAgentRuntimeTest`；
- `ContextAssemblerTest` / `ChatServiceTest` 替身 → `RecordingAgentExecutor` / `EchoAgentExecutor`；`ChatControllerTest` 同步新协议断言。


## 3. 当前架构（HEAD + `3dd3d47` 后）

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

## 4. 文档状态

### 4.1 既有工程文档（上一轮已同步）

- `AGENTS.md`：目录树 agent/llm 块、包依赖单向、职责链（AgentExecutor）、测试分层措辞；
- `README.md`：目录树 + 职责链（AgentRuntime → AgentExecutor）；
- `docs/CONTEXT.md`：顶层包分层表述、auto 回退链（无 mock）、`com.openalice.llm.LlmModelFactory` 路径；
- `docs/代码学习导览 v0.1.md`：表头升 v0.8 + §0.1 红字提示「§2~§7 结构/命名章节滞后，正文仍写旧 runtime/llm 结构，待下次完整重写」；
- `handoff.md`（本文件）。

### 4.2 本轮新增

- `_bmad-output/forge/openalice-product-idea/`：Forge 会话记录、极简 `forged-idea.md` 与自包含 HTML 报告；
- `handoff.md`：刷新当前快照并记录产品压力测试结论。
- **仍未新增项目级产品文档**：`forged-idea.md` 是 Forge 产物，不等同于正式 vision / roadmap / SPEC；下一轮应先做项目自有产品层文档，再继续代码里程碑。
- **未新增 ADR**：上一轮结构整理仍应在下轮补 **ADR 14「Agent 执行层命名与结构整理」**（记录 AgentExecutor / AgentScopeReActAgent / llm 顶包 / tool/ / mock 移除），并同步 `docs/index.md` 变更记录。

## 5. 红线与约定

- Java 21（本机默认 shell 是 Java 17，编译需先 `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`）。
- Spring Boot 3.5.16；AgentScope Java 2.0.2（core + extensions-model-openai，无 harness）；单 Maven 模块；不引入 Spring AI。
- 不提前创建空包 / 空模块 / 无人引用死代码；`web/` 不进入 Maven。
- 公开安全：不提交真实 key、个人邮箱、本机绝对路径、组织信息、构建产物；`application-local.yml` 含真实 key，只读不改不提交。
- Git：commit 需用户明确指示；push 一律用户手动执行。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。

## 6. 下一步

1. **Git 同步已完成**：Forge 产物与 handoff 已 push；后续 commit 仍按仓库约定等待用户明确指示再提交/推送。
2. **把 Forge 结论转成项目自有产品层文档（当前最高优先）**：以 `forged-idea.md` 为输入，形成 vision / roadmap / 第一份 SPEC，补齐“为什么做、先做哪个、如何验收”。
3. **统一人格契约**：修正需求书、人格资产与 `application.yml` 的冲突；当前通用 `warm and attentive` 与已锁定的天童爱丽丝行为核心不一致。人格卡、OpenHanako 参考、训练/数据集暂缓。
4. **补 ADR 14 + `docs/index.md` 变更记录 + 重写《代码学习导览》§2~§7**。
5. **再进入 M1 PostgreSQL** `session_message` 持久化（按 `StoredMessage` 建模：id/user_id/session_id/role/content/created_at，落 `chat.store.postgres`）。
6. 可讨论项：AgentScope 官方同 (user,session) 串行语义 vs 自研 `SessionCoordinator` 是否有重叠（暂未深究）；第一分享对象成功指标留待真实使用验证。

### 6.1 开发流程工具（本机环境，非仓库状态）

用户已在本机 Codex 安装 **BMAD-METHOD** 插件（`bmad-method` + `bmad-toolbox`，29 个 skill），用于补齐「产品澄清 → 需求 → 架构 → 实施」角色化流程。注意：

- 走的是**本地冻结副本** `~/.codex/marketplaces/bmad-plugins`（git 直连超时，手动 clone），**不会自动更新**；
- 安装版本为预发布 `6.13.0-next`（稳定版为 v6.12.0），如需收紧需手动换版；
- skill 仅在**会话启动时加载**，装完需新开 task 才可见；
- BMAD 会倾向生成它自己的 `docs/prd.md` / `docs/architecture.md` 体系。**不要直接覆盖现有 ADR 体系**——ADR 是技术决策史，BMAD 只用于补产品层。

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
