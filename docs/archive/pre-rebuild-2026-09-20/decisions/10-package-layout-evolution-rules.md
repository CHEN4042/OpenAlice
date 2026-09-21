# ADR 10 · 业务包自治与演进触发规则

- 状态：✅ 当前有效
- 日期：2026-09-08
- 关系：修订 ADR 09 的顶层物理布局（§2.1）；ADR 09 的语义结论（Turn / AgentEvent / 单用户 API / ContextAssembler / SessionCoordinator）全部保留

## 1. 背景

P1.5 编码完成后，与用户讨论了「文件放哪里」的物理结构问题。用户偏好本地 Jarvis 的 `pet/enums` 形态——**业务包自治，包内再按类型整理干净**；同时明确反对全局 `enums/` 大杂烩（改一个业务枚举牵动全局、看名字不知道属于谁）。

结论：这不是「类型优先 vs 业务优先」的二选一，而是**两级组织**：

```text
第 1 层（外层）按业务 / 角色分包；
第 2 层（内层）业务包内按类型子包整理（enums/ model/ service/ store/ ...）。
```

## 2. 决策：包结构规则

### 2.1 判归属（最重要的规则）

写新类型前先问：**「它只属于这个业务，还是跨业务共享？」**

- 只属于本业务 → 放进业务包（如未来 `memory/` 自己的枚举、模型）；
- 跨业务共享 → 上提共享区（当前为 `model/`，见 §2.2）；
- 禁止为「好看」把私有类型强行集中到全局 `enums/` / `constants/`；**默认留在本地，被逼无奈才上提**。

### 2.2 顶层按角色分

- **业务包**：一个可独立演进的业务一个包（当前只有 `chat`；未来 `memory` / `persona` 同级新增）；
- **内核包**：`agent`（AI 调用能力，被多个业务复用）；HTTP 入站适配 `controller` + `dto`；组合根 `config`；
- **共享包**：`model`——目前被 chat 业务与 agent 内核同时消费的对话词汇（ChatMessage / ConversationTurn / UserId / SessionId / MessageRole / TurnStatus），因此上提共享；若未来共享值类型族变多，再演进为 `common/` 族，现阶段不提前改名。

### 2.3 包内按类型再整理

业务包内部用类型子包表达「这层是什么」，业务归属已由外层表达。命名取业务惯用词：

```text
chat/
├── service/    # 编排：ChatService · ContextAssembler · SessionCoordinator
└── store/      # 持久化边界：ConversationStore · memory/InMemoryConversationStore
```

- 子包只允许表达类型 / 角色（service、store、domain、enums、model、rule……），禁止 `misc/`、`util/` 式垃圾桶；
- 同类文件少（约 <3~5 个）时可先平放，多到找文件费劲再拆子包——子包是整理手段，不是必须完成的仪式。

### 2.4 业务包之间不串门

- 业务只依赖共享 `model` 与内核 `agent`；业务之间若出现依赖，说明被依赖部分应上提共享或拆成新内核；
- 新业务加入时遵循既有边界，不反向依赖既有业务内部；
- Java 编译器不强制包边界，靠本规则 + 代码评审（必要时可加 ArchUnit）维护。

## 3. 当前布局（落地）

```text
src/main/java/com/openalice/
├── OpenAliceApplication.java       # 启动类（组合根 = Spring 容器）
├── model/                          # 共享词汇：ChatMessage · ConversationTurn · MessageRole · TurnStatus · UserId · SessionId
├── agent/                          # 内核：AI 调用
│   ├── AgentRequest.java · AgentEvent.java · TextDeltaEvent · DoneEvent · ErrorEvent
│   ├── runtime/                    # AgentRuntime(端口) · AgentScopeAgentRuntime(适配器) · Factory · Properties
│   └── llm/                        # LlmProvider · LlmModelFactory · DeterministicChatModel · ConfiguredHttpTransport
├── chat/                           # 业务：对话
│   ├── service/                    # ChatService · ContextAssembler · SessionCoordinator
│   └── store/                      # ConversationStore · memory/InMemoryConversationStore
├── controller/                     # HTTP/SSE 翻译（入站适配）
├── dto/                            # API 出入参
└── config/                         # 组合根：OpenAliceConfiguration
```

依赖方向：

```text
model ← chat.store
model ← agent
chat.service → model + chat.store + agent
controller → chat.service + dto
config 组装 chat.store + agent.runtime
```

## 4. 演进触发器（什么时候再动结构）

结构只在「被证明需要」时演进，不为想象中的未来提前搬家：

1. **第一个非 chat 业务包出现**（memory / persona / learning 立项）→ 同级新建 `com.openalice.<feature>`，内部同样按 §2.3 组织；
2. **第二个入站协议出现**（CLI / 语音）→ 才把 `controller` + `dto` 收敛为更明确的入站适配包（如 `web`）并考虑端口化；
3. **顶层目录接近 ~10 个** → 先治理归类，再评估是否拆 Maven 模块（ADR 08 结论不变）；
4. 触发前不搬家、不建空包（AGENTS 红线：语音 / learning / 插件 / 多 Agent 只保留架构位置）。

## 5. 与既有 ADR 的关系

- ADR 09（语义与包结构）：Turn 生命周期、AgentRequest/AgentEvent、ContextAssembler、SessionCoordinator、单用户 API、ConversationStore 语义不变；
- 顶层目录调整：`repository/` → `chat/store/`、`service/` → `chat/service/`；`model/`、`agent/`、`controller/`、`dto/`、`config/` 位置不变；
- ADR 08（单模块 + service 编排）、ADR 05（开发规范与测试策略）继续有效。

## 6. 影响与验证

- 纯物理移动 + import 改写，无行为 / API / 端点变更；
- 测试镜像同步移动（`src/test/java/com/openalice/chat/...`）；
- 验证：`mvn clean test`（Java 21）→ 22 tests / 0 failures / 0 errors；
- 回滚：恢复 ADR 09 布局（`git revert` 本变更）即可，语义层不受影响。
