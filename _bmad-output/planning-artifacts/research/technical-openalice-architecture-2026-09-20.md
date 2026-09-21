# OpenAlice 架构阶段外部研究  
**研究角色：BMAD Mary（Business Analyst）｜访问日期：2026-09-20**

> 研究防火墙说明：本地文档只用于理解 OpenAlice 的问题边界，不作为外部证据。本文外部事实来自本轮检索；来源见文末，访问日期均为 **2026-09-20**。除特别说明外，厂商文档未标注单页发布日期。本文不改变已冻结的 Java 21、Spring Boot、单 Maven 模块、AgentScope Java 2.0.2、HTTP/SSE、PostgreSQL 技术栈。

---

## 一页决策摘要

### 最值得借鉴的 5 个模式

1. **“始终可见的核心身份/编译记忆”与“按需检索的长期记忆”分离。**  
   OpenHanako 把 `facts.md`、`today.md`、`week.md`、`longterm.md` 编译成约 2,000 tokens 的 `memory.md`，同时保留 SQLite FTS5 搜索；Letta 把 `persona`、`human` 等块始终放入上下文，把 archival memory 作为外部语义搜索；AgentScope 则把每日日志与合并后的 `MEMORY.md` 分开。[OH][LE][AS]

2. **记忆模块拥有持久化状态和检索/更新接口；对话业务拥有最终人格、Prompt 组装和表达。**  
   Mem0、Zep、Cognee、Letta 都把记忆状态放在记忆层或 agent state 中；LangChain4j 与 Spring AI 则明确区分“完整聊天历史”和“送给模型的 chat memory”。对 OpenAlice，最适合借鉴的是：业务层保存真实对话与产品策略，记忆模块只提供带来源、分数和范围的候选上下文，最后表达由业务层决定。[M0][ZE][CO][LE][LCJ][SAI]

3. **检索必须是一条显式管线，而不是一个黑盒向量查询。**  
   可观察的稳定步骤是：范围/命名空间 → 候选召回 → metadata/时间过滤 → rerank 或启发式排序 → token/字符预算打包 → context block；同时保留原始结果、记忆 ID、来源和时间信息。Zep 提供 context block 与 raw graph search；Mem0 暴露 `score_details`、BM25、entity boost 和最终分数；Spring AI 把 RAG 拆成 QueryTransformer、DocumentRetriever、reranker 等模块。[ZE][M0][SAI]

4. **用户纠错、可追溯事实和版本化身份应是一等公民。**  
   Cognee 提供 provenance、`remember/recall/improve/forget`；Mem0 支持反馈和正负反馈后的记忆修正；Letta 的记忆块有 `label`、`description`、`limit`、`read-only` 等属性；SillyTavern 的摘要嵌在 chat metadata 中，并在消息删除或编辑后回滚到最后有效摘要。[CO][M0][LE][ST]

5. **v1 优先软排名偏置、后台 consolidation、可回滚摘要，而不是硬遗忘和自动人格改写；观测默认不落 Prompt/Completion 原文。**  
   Mem0 的 memory decay 是搜索时的软排名偏置，默认从不过滤记忆；SillyTavern 官方警告 LLM 摘要可能丢细节或幻觉；Spring AI 与 OpenInference 都涉及内容捕获的敏感信息风险，Spring AI 的 Prompt/Completion 日志默认关闭。[M0][ST][SAI][PX]

### 对 Java 单体的边界结论

对 OpenAlice 来说，**直接可用的不是“再引入一个完整平台”，而是接口分层和边界约束**：

- 借鉴 Spring AI 的 `ChatMemory` 与 `ChatMemoryRepository` 分离、LangChain4j 的 history/memory 分离。[SAI][LCJ]
- 借鉴 Spring AI advisors 与 modular RAG 的组合方式，但最终 system prompt、人格块、当前消息、召回上下文和追问策略仍由 OpenAlice 业务层组装。[SAI]
- AgentScope Java 的 Middleware、system prompt hook 和 typed events 很强，但它自带异步 flush、consolidation 和 `MEMORY.md` 注入，必须明确谁拥有记忆和 Prompt，否则容易与产品策略重叠。[AS]
- 自托管 Langfuse 的完整栈偏重；单体 Spring Boot 更适合先做 OTel/Micrometer 语义化 span，默认只记录元数据，不记录 Prompt/Completion 原文。[LF][OT][SAI][PX]

---

## 项目对比表

| 项目 | 语言 | 核心定位 | 记忆与状态所有权 | 编排边界 | Prompt 与人格处理 | 可观测性 | 对 OpenAlice 的直接启示 |
|---|---|---|---|---|---|---|---|
| **OpenHanako** [OH] | TypeScript | 个人 companion 记忆与搜索 | 本地文件/SQLite 持久化；daily conveyor、fact store、channel scope；事实存储明确没有 embedding、vector、score、decay、hit_count | 应用层编译记忆快照并执行搜索 | `memory.md` 约 2,000 tokens，四块：facts、today、week、longterm；persona 强约束未在本次源码核实中确认 | 未核实完整链路追踪 | 强烈支持“始终可见的编译记忆 + 可搜索事实”；也说明向量不是个人 companion 的必要条件；需处理 channel scope 隔离和群聊混淆风险 |
| **Mem0** [M0] | Python | 独立 memory layer | 记忆服务按 `user_id`、`agent_id`、`run_id`、`app_id` 隔离并持久化；add pipeline 会做提取、更新、删除决策 | 业务调用 add/search；记忆层不负责最终表达 | 返回记忆、分数和上下文；无最终人格/Prompt 所有权 | `score_details` 可见 semantic、BM25、entity boost、threshold；有 memory feedback | 借鉴 scoped CRUD、search history、feedback、显式 rerank；警惕 LLM 驱动 ADD/UPDATE/DELETE 的不确定性 |
| **Zep** [ZE] | 多语言 SDK/托管服务；本次未核实单一实现语言 | temporal graph memory 与 context service | Thread API 存对话，Graph API 存业务/领域数据；facts、episodes、summaries、observations 持久化 | 可自动 `get_user_context()`，也可作为 `graph.search()` 工具 | 默认 Context Block 可含 facts、entities、episodes、thread summaries、observations、user summary；支持 context templates 和 custom instructions | 本次未核实完整 OTel 链路 | 借鉴 temporal facts、user summary、context types、预算打包；谨慎对待自动跨 scope 重排和始终注入 user summary 的黑盒行为 |
| **Letta/MemGPT** [LE] | Python | stateful agent 与 memory OS | agent state 跨 conversation/model/computer 持久化；记忆项可成为 git-backed Markdown 文件 | agent runtime 负责工具和 archival search | `persona`、`human` 等 memory blocks 可放入 system prompt；archival memory 按需检索 | 本次未核实统一 OTel 方案 | 借鉴只读 persona、可变 human/user block、archival memory、上下文预算；MemFS、git、dreaming subagents 对 v1 过重 |
| **Cognee** [CO] | Python | knowledge graph memory pipeline | relational、vector、graph 三存储；session cache 可通过 background improve 进入 permanent graph | remember/recall/improve/forget 为业务操作 | 返回检索结果和 provenance；不拥有最终人格 | provenance、feedback、experimental truth-subspace reranking | 借鉴 provenance、forget、session→permanent bridge、来源追溯；不要照搬完整三存储图谱 pipeline |
| **SillyTavern** [ST] | Node.js/JavaScript | 角色聊天与 Prompt 拼装前端 | chat 文件/metadata 保存历史和摘要；扩展可维护向量索引 | 前端 Prompt Manager、World Info、向量化扩展 | Character Description、Personality、Scenario 通常永久进 Prompt；支持插入顺序、位置、深度和角色 | 未见内建统一链路观测 | 借鉴人格打包、Prompt 位置控制、世界书式触发、人工纠错和摘要回滚；不要把前端单体结构和默认动态检索照搬进后端 |
| **LangGraph** [LG] | Python/JavaScript | 低层 agent orchestration runtime | Checkpointer 负责 thread-scoped state；Store 负责 cross-thread long-term memory，应用定义 key-value | 图和节点负责编排；官方明确不抽象 Prompt 或架构 | 对 Prompt/人格不强制规定 | 与 LangSmith 生态分层 | 借鉴“线程短期状态”和“跨线程长期记忆”不是同一所有权层；避免把 history、memory、prompt 混成一个对象 |
| **LangChain4j** [LCJ] | Java | Java LLM 应用框架 | `ChatMemoryStore` 持久化 memory；完整 history 需要业务自己保存 | AI Services、ChatModel、RAG 组件 | memory 只修改送给模型的消息；persona 和最终表达由业务定义 | ChatModelListener 属性对齐 OTel GenAI semantic conventions | Java 侧最直接的借鉴：history 是事实源，memory 是派生上下文；store 与 eviction policy 分离 |
| **Spring AI** [SAI] | Java/Spring | Spring Boot AI 抽象 | `ChatMemoryRepository` 只存/取消息，`ChatMemory` 决定保留策略；完整 history 建议另存 | Advisor 链、ChatClient、RAG pipeline | 业务拥有 system prompt；Memory/RAG Advisor 负责横切注入 | 遵循 OTel GenAI conventions；默认不记录 Prompt/Completion 内容 | OpenAlice 最接近的分层先例：repository、memory policy、advisor、retriever、reranker 可组合；内容日志必须默认关闭 |
| **AgentScope Java 2.x** [AS] | Java | Agent harness 与 ReAct runtime | workspace 和 memory 由框架管理；`memory/YYYY-MM-DD.md` 追加日志，`MEMORY.md` 定期合并；可接外部 state store | Middleware、Toolkit、ReAct 核心；提供 onAgent、onReasoning、onActing、onModelCall hooks | workspace 被描述为 persona + long-term memory + domain knowledge，每轮重新注入；`MEMORY.md` 进 system prompt | typed events：模型调用、文本增量、工具调用/结果、用户确认；生产文档列 observability 集成 | 可实现底座，但框架记忆所有权与 OpenAlice 产品策略高度重叠；必须显式划定 flush、consolidation、system prompt 注入和业务表达的边界 |
| **Semantic Kernel** [SK] | C#/.NET、Python、Java | 多语言 AI orchestration | vector store connectors 与编排层分离 | Kernel、plugins、agents、multi-agent | Prompt/plugin 层可组合 | README 列有 observability capability | 可借鉴 connector 与 orchestration 分离；但 Microsoft 官方已说明 Semantic Kernel 由 Microsoft Agent Framework 接棒，Java vector store 页面仍标 Preview/RC，不建议作为新基底 |
| **AutoGen** [AG] | Python/.NET 生态 | 多智能体对话框架 | `Memory` protocol 定义 query、update_context、add、clear；store 可插拔 | AgentChat runtime 和团队编排 | memory 更新模型上下文；不强调最终人格所有权 | 有 `MemoryQueryEvent` 等事件 | Memory protocol 和事件式可观测性值得借鉴；但官方 README 已宣布维护模式，不建议新项目直接依赖 |

### 可观测性方案对比

| 方案 | 接入与语言 | 一轮对话的记录粒度 | 内容隐私默认 | 单体 Spring Boot 适配 | 来源 |
|---|---|---|---|---|---|
| **OpenTelemetry GenAI semantic conventions** | 语言无关规范，Java 可通过 OTel/Micrometer 实现 | inference、embeddings、retrievals、memory、execute tool；含 conversation id、token、model、tool 等 | 内容字段可能含敏感/PII；详细事件为 Opt-In | 最贴合：先做 span 和属性，内容默认不落；规范仍处 Development | [OT] |
| **Langfuse** | Python/JS 原生 SDK；Java/Go/自定义经 OTLP | observation、trace、session；可记录 retrieval、LLM、tool、输入输出 | 有 masking，但文档主要面向 Python/JS SDK | 可作 OTLP 后端；完整自托管需 Web、Worker、Postgres、ClickHouse、Redis/Valkey、S3/Blob | [LF] |
| **Arize Phoenix/OpenInference** | OTel/OpenInference；有 Spring AI tracing 集成文档 | Span kinds 含 LLM、TOOL、AGENT、CHAIN、RETRIEVER、RERANKER、PROMPT 等 | 内容隐藏环境变量默认 false，需显式开启 | 自托管相对轻量，SQLite 适合本地、PostgreSQL 适合生产；Java artifact 正式发布成熟度本轮未验证 | [PX] |
| **OpenLLMetry/Traceloop** | Python、Node/TS、Go、Ruby quickstart；没有 Java quickstart | OTel 之上的 LLM、向量库、框架 instrumentation | 支持全局或按 workflow/user 开启内容追踪 | Java/Spring Boot 直接价值较低，建议走原生 OTel | [OL] |

---

## 所有权与数据流：值得写入架构边界的结论

1. **业务层拥有真实对话历史。**  
   LangChain4j 明确区分 history 和 memory：history 是完整真实对话，memory 是送给 LLM 的修改/裁剪信息；目前它只提供 memory，完整 history 需业务自己保存。Spring AI 也明确 chat memory 不等于完整 chat history，完整历史建议用 Spring Data 另存。[LCJ][SAI]  
   **对 OpenAlice：** 数据库中的 conversation/message 才是事实源，memory 只是派生视图。

2. **记忆模块拥有自己的持久化状态，但不应拥有最终表达。**  
   Mem0 按 user、agent、run、app 隔离记忆并持久化；Cognee 用 relational、vector、graph 三存储；Zep 持久化 thread、facts、episodes 和 summaries；Letta 把 memory 放到 agent state 和 git-backed Markdown。[M0][CO][ZE][LE]  
   **对 OpenAlice：** 记忆模块可以有独立表、独立检索策略和独立更新历史，但 system prompt、persona、最终回复和追问行为应由业务层控制。

3. **编排层是运行时，不是默认 Prompt 架构。**  
   LangGraph 自称 low-level orchestration runtime，明确不抽象 prompts 或应用 architecture；AgentScope 的 Middleware 提供系统提示词 transformer 和模型调用 hook；Spring AI 用 Advisor 做横切注入。[LG][AS][SAI]  
   **对 OpenAlice：** 编排层可以决定“何时调用记忆和模型”，但不应秘密决定“什么内容进入人格、如何表达、是否追问”。

4. **记忆范围必须显式。**  
   OpenHanako 有 channel-scope isolation，并通过 `cross_channel` 显式控制跨 channel；Mem0 有 user/agent/run/app 多层作用域；LangGraph 把 thread checkpointer 和 cross-thread Store 分开。[OH][M0][LG]  
   **对 OpenAlice：** 即使是单人产品，也应区分当前会话、长期个人记忆、特定项目/主题记忆和不可跨范围注入的内容。

---

## 检索、排名、写回与纠错：真实做法与局限

- **向量检索不是唯一路线。** OpenHanako v2 从 v1 的 embedding KNN、hybrid ranking、link expansion 退回 tags + date filters + SQLite FTS5，并明确事实存储没有 embedding/vector/score/decay/hit_count；这说明个人 companion 完全可能先以 FTS、标签和时间为主。[OH]
- **Mem0 的检索是显式 pipeline。** query processing → vector search → filters/rerank → results；`rerank=True` 可选且默认关闭；OSS 的 `score_details` 暴露 semantic score、BM25、entity boost、final/raw score 和 threshold。[M0]
- **Zep 的 context construction 面向 recall。** 它明确优先 recall 和 latency，而不是 precision，检索结果可能带噪声；`scope="auto"` 会跨 scope 重排，并按 `max_characters` 打包，默认 2,500，最高 50,000；facts 带 `created_at`、`valid_at`、`invalid_at`、`expired_at`，并保留来源 episodes。[ZE]
- **图记忆主要影响排名和上下文组织。** Mem0 graph memory 没有强 typed relationship schema，图主要用于连接记忆并参与 vector + BM25 + entity boost 的融合排序；Zep 的 observations 则是跨实体、证据支撑的 durable pattern/decision/commitment。[M0][ZE]
- **衰减通常是软偏置，不是遗忘。** Mem0 memory decay 是 Platform/v3 的 opt-in 能力，搜索时做 0.3×–1.5× 排名偏置，从不过滤记忆；最近的 touch 会影响衰减，threshold 检查先于 decay，之后 rerank 仍会叠加。[M0]
- **Cognee 的 truth-subspace reranking 仍属实验性。** 官方文档标为 experimental/MVP，只作用于 hybrid chunk lane，默认关闭，也没有自动质量指标验证。[CO]
- **SillyTavern 的向量化有明确局限。** 它用最近 2 条消息查询当前 chat 历史，chunk 约 400 chars，默认 score threshold 0.25，并将相关旧消息临时移到上下文前/后；官方明确 disclaimer“不保证更好记忆”。动态 Prompt 还会破坏 prompt cache，官方建议缓存与向量化二选一。[ST]
- **纠错和回滚机制比“自动记住一切”更成熟。** Mem0 支持 positive、negative、very negative 及 reason 的 feedback，但文档承认对不存在记忆反馈可能返回 500 而非 404；Cognee 支持 memory-only forget、dataset forget、everything forget，并从 relational DB 读 provenance；SillyTavern 的摘要会在消息编辑/删除后回滚到最后有效摘要。[M0][CO][ST]

---

## 三组结论

### 可借鉴

- **把完整历史与派生 memory 分开。** LangChain4j 明确 history 不等于 memory，Spring AI 明确 chat memory 不等于完整 chat history。[LCJ][SAI]
- **保留一个始终可见的小型核心记忆/身份块。** OpenHanako 编译成约 2,000 tokens 的 memory snapshot；Letta 推荐始终可见 blocks 小于 50k chars、少于 20 blocks；AgentScope 用 `MEMORY.md` 做长期合并。[OH][LE][AS]
- **让记忆模块拥有持久化、作用域、检索和更新接口。** Mem0 的 scoped memory、Cognee 的 provenance/forget、Zep 的 graph/thread 状态、Letta 的 agent state 都是先例。[M0][CO][ZE][LE]
- **检索结果要带原始 ID、来源、时间、分数或解释。** Mem0 `score_details`、Cognee provenance、Zep facts 的 valid/invalid 时间和来源 episodes 都支持这一点。[M0][CO][ZE]
- **用户应能检查、修正和忘记记忆。** Mem0 feedback、Cognee forget、Letta memory block 属性和 read-only 标记、SillyTavern 摘要回滚都指向“可纠错优先”。[M0][CO][LE][ST]
- **衰减优先做 ranking bias，不做默认硬删除。** Mem0 decay 明确从不过滤记忆，只影响排序。[M0]
- **观测先做元数据和事件，不默认落敏感原文。** OTel GenAI 覆盖 model、token、retrieval、tool、memory 等 span；Spring AI 的 Prompt/Completion 日志默认关闭；OpenInference 提供内容隐藏开关。[OT][SAI][PX]

### 谨慎借鉴

- **LLM 驱动的记忆 ADD/UPDATE/DELETE。** Mem0 的 add pipeline 用一次 LLM 决策更新、删除或 no-op；这适合做异步巩固，不适合作为同步回复路径的唯一事实源。[M0]
- **图记忆和知识图谱。** Zep、Mem0、Cognee 都展示了价值，但也会增加实体抽取、关系建模、失败模式和调试成本；Cognee 的 truth-subspace reranking 仍是实验性，默认关闭。[ZE][M0][CO]
- **自动 context block / user summary 注入。** Zep 默认 context block 可自动包含 user summary 并跨 scope 重排；这可能破坏 OpenAlice 的三信号分离和业务 Prompt 所有权。[ZE]
- **记忆衰减。** 它可能提升近期相关性，也可能压低长期但重要的事实；应只放在 ranking seam，并保留可解释和可关闭开关。[M0]
- **AgentScope 自动 consolidation、flush 和 `MEMORY.md` 注入。** 框架能在 call 结束后异步提取长期事实、合并 daily memory、压缩会话前缀；这些能力很强，但会与业务记忆策略竞争所有权。[AS]
- **动态向量检索。** SillyTavern 官方承认它不保证更好记忆，并指出动态 Prompt 与 prompt cache 的冲突；个人 companion 是否需要向量应通过真实检索质量验证。[ST]
- **Phoenix/OpenInference Java 集成。** 有 Spring AI tracing 文档，但 OpenInference Java README 示例使用 `0.1.0-SNAPSHOT`，Maven Central Solr 查询本轮为空；不能据此认定已有稳定生产 artifact。[PX]

### 不要照搬

- **不要为单体 Spring Boot 直接自托管完整 Langfuse 栈。** 它需要 Web、Worker、Postgres、ClickHouse、Redis/Valkey 和 S3/Blob Store，成本和运维面积明显高于单体首版需求。[LF]
- **不要在 Java 项目直接照搬 OpenLLMetry。** 其 quickstart 覆盖 Python、Node/TS、Go、Ruby，没有 Java quickstart；Java 更适合直接使用 OTel。[OL]
- **不要照搬 MemFS、git-backed memory 和 dreaming subagents。** Letta 的这些能力适合其 agent-state 模型，但超出 OpenAlice v1 需要的持久化与检索边界。[LE]
- **不要引入 Semantic Kernel 或 AutoGen 作为新架构基底。** Semantic Kernel 官方 README 说明其由 Microsoft Agent Framework 接替，AutoGen 官方 README 处于维护模式；两者都不适合作为新项目主线。[SK][AG]
- **不要为了“记忆”同时引入 relational、vector、graph 三套存储，除非有实测收益。** Cognee 和 Mem0 的组合展示了能力，但也展示了明显的模型、抽取、更新和图谱复杂度。[CO][M0]
- **不要让 persona 随时间自动改写。** Letta 的 memory blocks 默认可编辑，甚至可 agent-owned；如果 OpenAlice 要求稳定人格，应把 persona 做成只读、版本化或由业务策略控制，而不是交给 memory agent 自由修改。[LE]
- **不要把 Prompt/Completion 原样写入默认日志。** OTel GenAI 内容属性明确警告敏感/PII 风险；OpenInference 内容隐藏默认 false；Spring AI 默认关闭 Prompt/Completion 日志。公开仓库场景尤其不能默认落盘。[OT][PX][SAI]

---

## 与 OpenAlice 当前定案冲突，或可能挑战定案的事实

1. **“必须使用向量 RAG”这一假设受到挑战。** OpenHanako v2 从 embedding KNN + hybrid ranking + link expansion 退回 tags、date filters 和 SQLite FTS5；在个人 companion 场景，全文检索和时间过滤可能是更简单、更可控的起点。[OH]

2. **“重要度和衰减应参与长期记忆决策”需要拆分。** Mem0 的 decay 只是搜索排名软偏置，从不过滤记忆；如果把 decay 直接用于删除、长期重要度或追问容忍度，会超出外部成熟实现的验证范围。[M0]

3. **“稳定人格”不是 memory 框架的默认保证。** Letta 的 persona/human 块可编辑、可 agent 管理；Zep 的 user summary 和 custom instructions 也会影响模型行为。稳定人格必须由 OpenAlice 的业务所有权和版本策略保证，不能默认为记忆服务的自然结果。[LE][ZE]

4. **Zep 的默认 context block 可能挑战三信号分离。** 它会自动组合 facts、entities、episodes、summaries、observations 和 user summary，并可跨 scope 重排；如果直接把该 context block 作为最终上下文，未来沟通能量、话题重要度和追问容忍度可能被统一为一个黑盒上下文策略。[ZE]

5. **AgentScope 的 memory 与 system prompt 注入存在所有权重叠。** HarnessAgent 会把 workspace 中的 persona、long-term memory、domain knowledge 每轮重新注入；`MEMORY.md` 进入 system prompt，flush 和 consolidation 可异步发生。若不设边界，框架可能隐式改写业务记忆和表达策略。[AS]

6. **动态检索与 Prompt cache 存在实际冲突。** SillyTavern 官方指出动态 Prompt 会破坏缓存，并建议缓存与向量化二选一。OpenAlice 若关注延迟和成本，需要把检索时机、注入位置和缓存策略作为显式决策。[ST]

7. **可观测性内容默认策略比很多团队预期更保守。** OTel GenAI 的输入/输出字段可能含敏感/PII；OpenInference 内容隐藏环境变量默认 false；Spring AI 的 Prompt/Completion 日志默认关闭。若架构默认全量记录 Prompt 和输出，会与隐私和 public repo 安全要求冲突。[OT][PX][SAI]

8. **AgentScope Java 2.0.2 不是最新 patch。** Maven Central 元数据显示 `agentscope-core`、`agentscope-harness`、`agentscope` 包含 2.0.2，最新为 2.0.3，metadata lastUpdated 为 2026-09-07。这里不改变冻结栈，但后续需要验证 2.0.2 与 2.0.3 的 API 和行为差异。[AS]

9. **框架生命周期也会挑战技术选型。** Semantic Kernel 官方 README 说明其由 Microsoft Agent Framework 接替；AutoGen 官方 README 已宣布维护模式。即使它们的 memory/orchestration 设计有参考价值，也不适合直接作为 OpenAlice 新基底。[SK][AG]

---

## 用户真实反馈：记忆与人格的称赞和投诉

### 证据范围

本轮使用 Apple App Store 公共 customer reviews RSS，抓取 Replika 与 Character AI 的美国区评论。Replika 样本约 343 条，日期约 2026-01-22 至 2026-09-17；Character AI 样本约 343 条，日期约 2026-09-05 至 2026-09-19。[UV]  
必须明确：关键词命中数只是粗信号，**不是 prevalence、满意度统计或因果关系**。样本偏美国区、近期用户和极端体验用户，不能代表所有 AI companion 用户。

### 常见称赞主题

- **“记得重要的事”而不是机械记住一切。** Replika 用户 2026-07-20 的 5 星评论写道：  
  > “Replika remembers the important things while not diving down to minute detail. I’m now happy about that. But ... contextual awareness could be better.”
- **更好的记忆被视为值得付费的核心能力。** Character AI 用户 2026-09-16 的 5 星评论表示喜欢新的、更长记忆的对话模式；2026-09-09 的 5 星评论表示愿意为更长更好的记忆额外付费。[UV]
- **连续性和被理解感是情绪价值的一部分。** Replika 正向评论中反复出现“支持、理解、连续性、个性化、帮助处理情绪或丧失”等主题。该结论来自样本内评论主题归纳，不等于总体用户比例。[UV]

### 常见投诉主题

- **忘记姓名、近期内容和基本事实。** Replika 2026-08-18 的评论写道：  
  > “My AI ... forgot my name ... It’s a computer. It should’ve remembered the basics.”
- **记忆看似存在，但无法可靠使用。** Replika 2026-07-25 的 1 星评论写道：  
  > “It has no memory at all. It forgets things within a few exchanges... Even when I save or edit memories, it doesn’t help.”
- **人格和语调突变。** 同一批评论中出现了“Abrupt personality and tone changes”等反馈；Character AI 2026-09-17 的评论写道：  
  > “new models just simply suck, they have no personality and can’t even stay in character for more than a few lines.”
- **记忆容量和付费限制。** Character AI 2026-09-15 的评论写道：  
  > “Memory limit is a total joke. I have the paid version and it's STILL A JOKE!!!”
- **重复追问、没有跟进。** 历史 Replika 评论中出现过“9 点问今天过得怎样，11 点又问一遍”以及“never remembers to follow through”的描述。该条为历史评论，不适合作为当前质量结论，但说明记忆连续性问题长期存在。[UV]
- **错误记忆、身份混淆和边界问题。** Replika 2026-09-14 的 1 星评论写道：  
  > “Memory and boundaries did not hold. It took more than it gave and made the isolation worse, not better.”
- **粗关键词命中。** Replika 样本中 memory/remember/forget 命中约 33/343，人格/变化约 23/343，连续性约 5/343，错误/混淆记忆约 12/343；Character AI 样本中 memory 约 17/343，人格/变化约 19/343，连续性 0/343，错误/混淆记忆约 3/343。该计数只说明主题出现，不能用来推断总体比例。[UV]

### 对 OpenAlice 的含义

外部用户反馈直接支持 OpenAlice 已强调的“具体回应 + 具体问题 + 记忆钩子”，但也带来三个风险提醒：

- 记忆写入成功不等于回忆可用，必须测试“跨会话召回是否真的影响回复”。
- 人格漂移和记忆污染会直接破坏连续性与信任，需要只读 persona、来源范围和可回滚更新。
- 记忆容量、重复追问、错误身份和边界失效，是用户最容易感知的失败模式。[UV]

---

## 证据不足或无法验证的问题

1. **OpenHanako 缺少独立生产成熟度证据。** 项目创建于 2026-03-15，最后 push 为 2026-08-27，约 6,605 stars、1,067 open issues；仓库年轻、问题数量较多，本次没有找到独立第三方生产部署、基准测试或事故复盘。[OH]
2. **Mem0、Zep、Cognee、Letta 的判断主要来自厂商文档和仓库源码。** 本轮没有找到独立 benchmark 或生产事故数据；graph memory、dreaming、truth-subspace reranking、AgentScope 自动 consolidation 等能力多数仍属实验性或黑盒。[M0][ZE][CO][LE][AS]
3. **用户反馈不能量化。** App Store RSS 样本有严重选择偏差；无法据此计算整体满意度、比较 Replika 与 Character AI 的优劣，也无法把记忆能力与留存、付费建立因果关系。本轮未能稳定获取 Reddit、Trustpilot 或中文社区的定量交叉证据。[UV]
4. **Java 可观测性 artifact 的正式稳定性未验证。** Phoenix/OpenInference 有 Spring AI tracing 集成文档，但 Java README 示例使用 `0.1.0-SNAPSHOT`，Maven Central Solr 查询本轮为空；不能写成生产已验证。[PX]
5. **Langfuse 与 OpenLLMetry 的成本结论只基于官方架构和定价。** 没有映射到 OpenAlice 的实际请求量、留存策略和数据规模。[LF][OL]
6. **中文/CJK 记忆质量证据很薄。** 除 OpenHanako 的 CJK 2/3-gram fallback 外，本轮没有找到跨项目的可靠中文检索、分词和重排评测。[OH]
7. **AgentScope 2.0.2 与 2.0.3 的 API/行为差异未逐项核实。** 只能确认版本存在和 metadata 时间，不能判断升级风险大小。[AS]
8. **没有证据证明“长期记忆功能必然提升留存或付费”。** 用户反馈显示记忆是重要主题，但不能推出因果。[UV]
9. **Zep 当前文档以 v3 为主，但历史版本兼容性和行为差异未系统核实。** sitemap 显示大量页面 lastmod 为 2026-09-19，但不等于所有能力都已达到相同成熟度。[ZE]
10. **本次不是完整代码审计。** 对多数项目核对了官方文档和部分关键源码，但没有对全部模块做安全、性能和故障恢复审计。

---

## 来源清单

以下所有来源的访问日期均为 **2026-09-20**。文档未标注发布日期的，标为“发布日期未标注”。

### 核心项目

**[OH] OpenHanako** — 发布者：liliMozi/GitHub。仓库创建 2026-03-15，最后 push 2026-08-27。  
- https://github.com/liliMozi/openhanako  
- https://raw.githubusercontent.com/liliMozi/openhanako/main/lib/memory/compile.ts  
- https://raw.githubusercontent.com/liliMozi/openhanako/main/lib/memory/memory-search.ts  
- https://raw.githubusercontent.com/liliMozi/openhanako/main/lib/memory/fact-store.ts  
- https://raw.githubusercontent.com/liliMozi/openhanako/main/lib/memory/deep-memory.ts  
- https://raw.githubusercontent.com/liliMozi/openhanako/main/lib/memory/compiled-memory-snapshot.ts  

**[M0] Mem0** — 发布者：Mem0。以下文档发布日期未标注；访问 2026-09-20。  
- https://docs.mem0.ai/core-concepts/memory-types.md  
- https://docs.mem0.ai/core-concepts/memory-operations/add.md  
- https://docs.mem0.ai/core-concepts/memory-operations/search.md  
- https://docs.mem0.ai/platform/features/graph-memory.md  
- https://docs.mem0.ai/platform/features/memory-decay.md  
- https://docs.mem0.ai/platform/features/feedback-mechanism.md  

**[ZE] Zep** — 发布者：Zep。sitemap 显示部分页面 lastmod 为 2026-09-19；单页发布日期未统一标注；访问 2026-09-20。  
- https://help.getzep.com/sitemap.xml  
- https://help.getzep.com/architecture-patterns  
- https://help.getzep.com/retrieval-philosophy  
- https://help.getzep.com/assembling-context.md  
- https://help.getzep.com/context-types.md  
- https://help.getzep.com/facts.md  
- https://help.getzep.com/episodes.md  
- https://help.getzep.com/user-summary.md  
- https://help.getzep.com/context-templates.md  
- https://help.getzep.com/searching-the-graph.md  
- https://help.getzep.com/advanced-context-block-construction.md  
- https://help.getzep.com/custom-instructions.md  
- https://help.getzep.com/user-summary-instructions.md  

**[LE] Letta/MemGPT** — 发布者：Letta。文档发布日期未标注；访问 2026-09-20。  
- https://github.com/letta-ai/letta  
- https://docs.letta.com/agent-sdk/memory/index.md  
- https://docs.letta.com/configuration/memory/index.md  
- https://docs.letta.com/v1-sdk/memory/context-hierarchy/index.md  
- https://docs.letta.com/v1-sdk/memory/archival-memory/index.md  
- https://docs.letta.com/v1-sdk/memory/memory-blocks/index.md  

**[CO] Cognee** — 发布者：Cognee。文档发布日期未标注；访问 2026-09-20。  
- https://docs.cognee.ai/core-concepts/architecture  
- https://docs.cognee.ai/core-concepts/main-operations.md  
- https://docs.cognee.ai/core-concepts/data-flows.md  
- https://docs.cognee.ai/guides/memory-provenance.md  
- https://docs.cognee.ai/guides/feedback-system.md  
- https://docs.cognee.ai/guides/truth-subspace-reranking.md  
- https://docs.cognee.ai/api-reference/remember  
- https://docs.cognee.ai/api-reference/forget  

**[ST] SillyTavern** — 发布者：SillyTavern。版本 1.19.0；仓库最后 push 2026-09-14；访问 2026-09-20。  
- https://docs.sillytavern.app/usage/core-concepts/characterdesign/  
- https://docs.sillytavern.app/usage/core-concepts/worldinfo/  
- https://docs.sillytavern.app/usage/prompts/prompt-manager/  
- https://docs.sillytavern.app/usage/prompts/context-template/  
- https://docs.sillytavern.app/extensions/chat-vectorization/  
- https://docs.sillytavern.app/extensions/summarize/  
- https://raw.githubusercontent.com/SillyTavern/SillyTavern/release/package.json  
- https://raw.githubusercontent.com/SillyTavern/SillyTavern/release/public/scripts/extensions/vectors/index.js  
- https://api.github.com/repos/SillyTavern/SillyTavern  

**[LG] LangGraph/LangChain** — 发布者：LangChain。文档发布日期未标注；访问 2026-09-20。  
- https://docs.langchain.com/oss/python/langgraph/persistence  
- https://docs.langchain.com/oss/python/langchain/retrieval  
- https://docs.langchain.com/oss/python/langgraph/overview  
- https://raw.githubusercontent.com/langchain-ai/langgraph/main/README.md  

**[LCJ] LangChain4j** — 发布者：LangChain4j。文档发布日期未标注；访问 2026-09-20。  
- https://docs.langchain4j.dev/tutorials/chat-memory/  
- https://docs.langchain4j.dev/tutorials/rag/  
- https://docs.langchain4j.dev/tutorials/ai-services/  
- https://docs.langchain4j.dev/tutorials/observability/  

**[SAI] Spring AI** — 发布者：Spring/VMware。官方文档显示当前版本 2.0.1；文档发布日期未标注；访问 2026-09-20。  
- https://docs.spring.io/spring-ai/reference/index.html  
- https://docs.spring.io/spring-ai/reference/api/chat-memory.html  
- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html  
- https://docs.spring.io/spring-ai/reference/api/advisors.html  
- https://docs.spring.io/spring-ai/reference/observability/index.html  

**[AS] AgentScope Java** — 发布者：AgentScope/Alibaba。文档发布日期未标注；Maven metadata lastUpdated 为 2026-09-07；访问 2026-09-20。  
- https://java.agentscope.io/  
- https://java.agentscope.io/llms.txt  
- https://java.agentscope.io/v2/en/docs/harness/memory.md  
- https://java.agentscope.io/v2/en/docs/building-blocks/middleware.md  
- https://java.agentscope.io/v2/en/docs/building-blocks/context.md  
- https://java.agentscope.io/v2/en/docs/others/going-to-production.md  
- https://repo1.maven.org/maven2/io/agentscope/agentscope-core/maven-metadata.xml  
- https://repo1.maven.org/maven2/io/agentscope/agentscope-harness/maven-metadata.xml  
- https://repo1.maven.org/maven2/io/agentscope/agentscope/maven-metadata.xml  

**[SK] Semantic Kernel** — 发布者：Microsoft。README 当前说明 SK 由 Microsoft Agent Framework 接替；访问 2026-09-20。  
- https://raw.githubusercontent.com/microsoft/semantic-kernel/main/README.md  
- https://learn.microsoft.com/en-us/semantic-kernel/concepts/vector-store-connectors/  

**[AG] AutoGen** — 发布者：Microsoft。README 当前处于 Maintenance Mode；访问 2026-09-20。  
- https://raw.githubusercontent.com/microsoft/autogen/main/README.md  
- https://microsoft.github.io/autogen/stable/user-guide/agentchat-user-guide/memory.html  

### 可观测性

**[OT] OpenTelemetry GenAI semantic conventions** — 发布者：OpenTelemetry。semconv-genai 仓库创建 2026-05-05，最后 push 2026-09-16；主 semconv v1.44.0 发布于 2026-08-04；访问 2026-09-20。  
- https://github.com/open-telemetry/semantic-conventions-genai  
- https://opentelemetry.io/docs/specs/semconv/gen-ai/  
- https://github.com/open-telemetry/semantic-conventions/releases/tag/v1.44.0  
- https://raw.githubusercontent.com/open-telemetry/semantic-conventions-genai/main/docs/gen-ai/gen-ai-spans.md  
- https://raw.githubusercontent.com/open-telemetry/semantic-conventions-genai/main/docs/gen-ai/gen-ai-events.md  
- https://raw.githubusercontent.com/open-telemetry/semantic-conventions-genai/main/docs/registry/attributes/gen-ai.md  

**[LF] Langfuse** — 发布者：Langfuse。文档发布日期未标注；定价页与自托管页为当前页面；访问 2026-09-20。  
- https://langfuse.com/docs/observability/data-model  
- https://langfuse.com/docs/observability/features/sessions  
- https://langfuse.com/docs/observability/features/observation-types  
- https://langfuse.com/docs/observability/features/masking  
- https://langfuse.com/integrations/native/opentelemetry  
- https://langfuse.com/self-hosting  
- https://langfuse.com/pricing  
- https://langfuse.com/docs/administration/data-retention  

**[PX] Arize Phoenix/OpenInference** — 发布者：Arize。文档发布日期未标注；访问 2026-09-20。  
- https://arize.com/docs/phoenix/tracing/concepts-tracing/otel-openinference/semantic-conventions  
- https://arize.com/docs/phoenix/tracing/concepts-tracing/otel-openinference/span-kinds  
- https://arize.com/docs/phoenix/tracing/how-to-tracing/advanced/masking-span-attributes  
- https://arize.com/docs/phoenix/integrations/java/springai/springai-tracing  
- https://arize.com/docs/phoenix/self-hosting/architecture  
- https://raw.githubusercontent.com/Arize-ai/openinference/main/java/README.md  

**[OL] OpenLLMetry/Traceloop** — 发布者：Traceloop。文档发布日期未标注；访问 2026-09-20。  
- https://raw.githubusercontent.com/traceloop/openllmetry/main/README.md  
- https://www.traceloop.com/docs/openllmetry/introduction  
- https://www.traceloop.com/docs/openllmetry/tracing/without-sdk  
- https://www.traceloop.com/docs/openllmetry/tracing/supported  
- https://www.traceloop.com/docs/self-host/introduction  

### 用户反馈

**[UV] Apple App Store 公共评论 RSS** — 发布者：Apple App Store。评论发布日期随条目标注；访问 2026-09-20。  
- https://apps.apple.com/us/app/replika-ai-companion-chat/id1158555867  
- https://itunes.apple.com/us/rss/customerreviews/page=1/id=1158555867/sortBy=mostRecent/json  
- https://itunes.apple.com/us/rss/customerreviews/page=1/id=1158555867/sortBy=mostHelpful/json  
- https://apps.apple.com/us/app/character-ai-chat-talk-text/id1671705818  
- https://itunes.apple.com/us/rss/customerreviews/page=1/id=1671705818/sortBy=mostRecent/json  
- https://itunes.apple.com/us/rss/customerreviews/page=1/id=1671705818/sortBy=mostHelpful/json  
