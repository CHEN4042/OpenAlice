# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `docs/index.md`；只按需读取必要文档。
> 更新要求：每次会话结束刷新当前状态、验证结果、未决问题与下一步。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-20 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | P1 后端可运行；上一功能 HEAD `7e445bc`；本轮完成 **BMAD SPEC + ADR 14 + M1 PostgreSQL 持久化 + Alice 角色 prompt**，已由本轮 commit 收口 |
| 分支 | `main` |
| 当前架构 | 单 Maven 模块；根包 `com.openalice`；顶层 `model / agent / llm / chat(service\|store) / controller / dto / config`；`runtime/` 与 `agent.llm` 已拆除 |
| Git 状态 | 本轮基于 `7e445bc`；SPEC / ADR 14 / M1 工作区改动已由当前 commit 收口；`application-local.yml` 已 gitignore 且含本机真实 key，**绝不提交 / 不 cat / 不打印** |
| 验证 | `mvn -q clean test` 已通过（Java 21，2026-09-20 22:54）；`PostgresConversationStoreTest` 以 H2 PostgreSQL 模式覆盖跨实例恢复、会话隔离与最近 N 条顺序；本机无 Docker，尚未跑真实 PostgreSQL 冒烟 |
| 下一里程碑 | 用真实 PostgreSQL 验收启动、迁移和重启恢复；之后进入 `UserProfile` 等产品能力 |

## 1. Forge 结论（已完成）

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

- Forge 当时发现：需求书 §0.4 把碧蓝档案爱丽丝列为主灵感，但 §5.1 仍是通用“温柔陪伴型”；`application.yml` 也只有 `warm and attentive AI companion`。该冲突后续已由 SPEC + ADR 14 统一。
- Forge 会话本身只改产品层记录；后续同一工作区继续完成了 SPEC、人格契约和 M1 实现。

### 1.5 后续收口：SPEC、ADR 14 与 M1 首个实施切片（当前工作区）

- `_bmad-output/specs/spec-openalice/SPEC.md`：把 `HARDENED` 结论收敛为 CAP-1–CAP-5、约束、非目标与方向性成功信号；
- `persona-contract.md` / `conversation-policy.md`：分别承载人格核心、四类异议与三信号动作规则，避免把长细则塞回 SPEC kernel；
- `docs/decisions/14-persona-contract-first.md`：首版人格用结构化 system prompt 落地，独立 `persona/` 资产、初始化向导、训练与数据集后置；
- M1 PostgreSQL：新增 `PostgresConversationStore`、Flyway `V1__create_session_message.sql`、Spring JDBC/Flyway/PostgreSQL 依赖、`compose.yaml` 与 `application-local.yml.example`；默认存储为 PostgreSQL，内存实现只保留测试/显式降级；
- 文档同步：需求书升至 v1.5.1，README / CONTEXT / 记忆设计 / 代码学习导览 / `docs/index.md` 已同步；
- Git：本轮改动已由当前 commit 收口。
- 未完成：真实 PostgreSQL 启动冒烟（本机没有 Docker）；`UserProfile`、CAP-5 算法和人格行为自动验收尚未实现。

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
├── compose.yaml                 # 本地 PostgreSQL（pgvector 镜像）
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
│   │   └── store/               # ConversationStore · StoredMessage · memory/ · postgres/PostgresConversationStore
│   ├── controller/              # ChatController · HealthController · ApiExceptionHandler
│   ├── dto/                     # ChatRequest(sessionId+message) · ChatStreamEvent · MessageView
│   └── config/                  # 组合根：OpenAliceConfiguration + OpenAliceSettings（绑定 openalice.*）
├── src/test/java/com/openalice/  # 镜像 main；含 InMemoryConversationStoreTest · PostgresConversationStoreTest · ChatControllerTest
└── resources/
    ├── application.yml          # 公共安全默认值；本机 key 在 gitignored application-local.yml
    └── db/migration/            # Flyway V1__create_session_message.sql
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
- `_bmad-output/specs/spec-openalice/`：5 项 capability、2 个 companion 与 `.memlog.md`；
- `docs/decisions/14-persona-contract-first.md`：当前人格实现决议，并修订 ADR 07 中的 persona 初始化时点；
- `PostgresConversationStore` + Flyway 迁移 + `compose.yaml` + local profile 模板：M1 首个真实持久化切片；
- `application.yml`：结构化 Alice 角色 prompt；测试里的通用人格常量改为中性测试值；
- 需求书、README、CONTEXT、记忆设计、代码学习导览、`docs/index.md` 与 `handoff.md` 同步。
- **待处理**：真实 PostgreSQL 冒烟、人格行为真实对话验收、CAP-4 `UserProfile` 与 CAP-5 算法均未完成。

## 5. 红线与约定

- Java 21（本机默认 shell 是 Java 17，编译需先 `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`）。
- Spring Boot 3.5.16；AgentScope Java 2.0.2（core + extensions-model-openai，无 harness）；单 Maven 模块；不引入 Spring AI。
- 不提前创建空包 / 空模块 / 无人引用死代码；`web/` 不进入 Maven。
- 公开安全：不提交真实 key、个人邮箱、本机绝对路径、组织信息、构建产物；`application-local.yml` 含真实 key，只读不改不提交。
- Git：commit 需用户明确指示；push 一律用户手动执行。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。

## 6. 下一步

1. **真实 PostgreSQL 冒烟（当前代码验收门槛）**：在具备 Docker / PostgreSQL 的机器上执行 `docker compose up -d postgres`，启动应用，确认 Flyway 创建 `session_message`；发一轮消息、重启应用并读取同一 session 历史。
2. **CAP-4 最小身份实现**：把 `ChatService` 私有默认用户提升为本地配置读取的便利函数，保持 API 不暴露 `userId`；新增可修改 `UserProfile`，不要提前造 provider / DI 层。
3. **CAP-1 / CAP-3 行为验收**：用理发、蛋糕、自我否定和坚持决定等真实场景对话，检查回复是否具体且留梯子、人格是否稳定、异议是否只说一次并交还决定权；先人工验证，不急于搭建自动评测。
4. **CAP-5 后置算法**：话题重要度衰减、跟进容忍度恢复和主动接回触发规则继续标记为论文 / 实验议题，不用临时分数硬编码。
5. 可讨论项：AgentScope 官方同 (user,session) 串行语义 vs 自研 `SessionCoordinator` 是否有重叠（暂未深究）；第一分享对象成功指标留待真实使用验证。

### 6.1 开发流程工具：BMAD-METHOD 与换机恢复

本项目用 BMAD 补齐「产品澄清 → 需求 → 架构 → 实施」角色化流程；技术决策史仍以 `docs/decisions/` 的 ADR 为准。当前已验证环境：

- Codex 插件：`bmad-method@bmad`、`bmad-toolbox@bmad`，均已安装并启用；
- 版本：`6.13.0-next`；
- marketplace 名：`bmad`；
- 本地 marketplace 源：`~/.codex/marketplaces/bmad-plugins`，已 checkout 到冻结 commit `d009608292d8a2ea4df846de7dca2f0d78a9e22d`（`feat(release): build method and toolbox from BMAD-METHOD`，2026-09-05）；
- 依赖：`uv` 可用（BMAD skill 的脚本通过 `uv run` 启动）。

换电脑后按以下顺序恢复。要复现当前环境，先 clone 并固定 commit，再安装插件：

```bash
mkdir -p "${CODEX_HOME:-$HOME/.codex}/marketplaces"
git clone https://github.com/bmad-code-org/bmad-plugins.git "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins"
git -C "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins" checkout d009608292d8a2ea4df846de7dca2f0d78a9e22d
codex plugin marketplace add "${CODEX_HOME:-$HOME/.codex}/marketplaces/bmad-plugins"
codex plugin add bmad-method@bmad
codex plugin add bmad-toolbox@bmad
codex plugin list
```

若只跟随 upstream，可改用官方远程安装（不保证复现当前 commit）：

```bash
codex plugin marketplace add bmad-code-org/bmad-plugins
codex plugin add bmad-method@bmad
codex plugin add bmad-toolbox@bmad
```

恢复注意事项：

- skill 只在 Codex task 启动时加载；安装后要新开 task；
- 当前仓库只使用 BMAD 管理产品层，不直接采纳其 `docs/prd.md` / `docs/architecture.md` 布局，避免覆盖现有 ADR；
- 当前项目尚未生成 `_bmad/`；执行下游 BMAD skill 前，在新 task 中先运行 `bmad setup`（或等价调用）初始化项目脚本与配置。若只想保留 `_bmad-output/` 产物而不同步项目内 BMAD runtime，则不把 `_bmad/` 纳入仓库，并在恢复后按该选择执行；
- 已执行 `bmad-forge-idea`：产物在 `_bmad-output/forge/openalice-product-idea/`，结论 `HARDENED`；
- 已执行 `bmad-spec`：以 `forged-idea.md` 为输入生成 `_bmad-output/specs/spec-openalice/`（5 项 capability + 2 companions），随后工作区继续落地 ADR 14 与 M1；
- 下一计划流程：不再继续补产品澄清；先做真实 PostgreSQL 冒烟，再按 SPEC 进入 CAP-4 或其他实施切片。

---
**End of Handoff** — 下一位接手者先读本文件，再动手。
