# OpenAlice Project Re-initialization Proposal

> 日期：2026-09-27
> 状态：**PROPOSAL / NON-AUTHORITATIVE**
> 目的：为架构阶段提供初始化方案；本文不能覆盖产品契约，也不能代替尚未定稿的 architecture spine。

## 1. 结论摘要

OpenAlice 应重新初始化，但不应把“重新初始化”理解为先搬目录或重写代码。当前最重要的工作，是先建立一条不会被工具、旧实现或临时会话反向污染的权威链，再以可执行的架构约束、评测资产和工程护栏重建实现。

建议采用以下总体方案：

1. 保留已经冻结的产品契约，不重新发明产品方向。
2. 将当前 `src/` 视为一次可追溯的 legacy baseline，而不是新实现的脚手架。
3. 首版运行形态采用单部署单元、单 Maven 模块的 capability-oriented modular monolith；不预建微服务、插件平台、多租户或高并发抽象。
4. 先定义状态所有权、依赖方向和端到端垂直切片，再决定类名、接口和表结构。
5. 把 evaluation、可观测性、安全和 Agent 开发工作流当作项目结构的一部分，而不是功能完成后的补充。
6. 默认采用顺序、可交接的 agent workflow；只有任务能真正隔离且合并边界明确时才并行。
7. 不在本轮删除、移动或改写 legacy 资产。任何清理都放在初始化方案获批且已有可恢复快照之后。

## 2. 本轮调查范围与事实基线

### 2.1 本地仓库

本轮阅读了当前权威文件、完整 Git 历史、SPEC companions、架构研究、BMAD 过程记录、历史归档、当前 Java 源码和测试。

重要事实：

- 当前分支为 `main`，调查开始时与 `origin/main` 对齐，HEAD 为 `38dbe0c`。
- SPEC 与三个 companions 已形成清晰的产品行为契约。
- `_bmad-output/architecture/openalice/ARCHITECTURE-SPINE.md` 仍是未填写的模板，当前不存在可执行的技术权威。
- Git 历史显示工程边界曾在多模块、单模块、包名和接口之间连续调整；这说明问题不在某个类，而在缺乏稳定且可机械验证的架构契约。
- 当前 `src/` 包含可参考的行为样例，但也包含与新产品契约不一致或尚未定案的结构，例如 session 概念、默认用户、内嵌 system prompt 和框架边界。
- 当前 PostgreSQL store 测试实际使用 H2 PostgreSQL compatibility mode，不能证明真实 PostgreSQL 行为。
- 仓库目前没有 CI workflow、Maven Wrapper、JDK toolchain/enforcer、依赖更新策略或正式的 evaluation 目录。
- 本机默认 Java 为 17，因此直接执行 Maven 测试失败；显式切换到 JDK 21 后，25 个现有测试全部通过。这说明代码基线可运行，但开发环境契约尚未被仓库强制执行。

### 2.2 外部一手资料得到的共同原则

- OpenAI 的 harness engineering 实践强调：`AGENTS.md` 应是地图而不是百科全书；结构化仓库文档是 system of record；架构不变量应由 lint、结构测试等机制执行；计划和技术债应成为一等资产；agent 必须能直接读取运行状态、日志和指标。
- OpenAI 的 agent evaluation 指南强调先用 trace 定位失败，再用可重复的数据集和评测运行阻止回归。
- Anthropic 建议从最简单、可组合的 workflow 开始，仅在收益能抵消成本与不可预测性时增加 autonomy；工具接口本身是 agent 成败的重要部分。
- Google ADK/Agents CLI 将应用、单元测试、集成测试、evaluation dataset/config 和部署配置分开管理；evaluation 同时检查最终结果与执行轨迹。
- AWS Agentic AI Lens 将 prompt/config 版本、工具最小权限、memory 隔离、trace、分层 eval、失败演练和 human oversight 作为同一工程体系。
- Microsoft Agent Framework 的可观测性建立在 OpenTelemetry traces、logs 和 metrics 上，关注消息流、执行器性能与错误。
- AgentScope Java 的 harness 会管理 session state、日志、prompt 构建与 middleware；OpenAlice 必须明确哪些只是框架运行态，不能把产品对话史、身份或记忆语义默交给框架。
- ReAct、MetaGPT、ChatDev 和 SWE-agent 分别证明了环境反馈、标准化中间产物、角色交接和 agent-computer interface 的价值；MAST 则提醒，多 agent 会新增规格理解、协作和验证失败模式，不能把“更多 agent”当成默认质量提升。

这些资料支持的是工程原则，不构成照搬任何框架目录或运行时的理由。

## 3. 新的文档权威模型

当前“handoff 高于 SPEC”的顺序不适合长期维护。`handoff.md` 是易变状态，不能覆盖稳定契约。建议 architecture 阶段同时把权威关系改为：

| 层级 | 资产 | 可以决定什么 | 不可以决定什么 |
| :-- | :-- | :-- | :-- |
| 1 | 产品契约 | 用户可观察行为、范围、验收红线 | 包结构、类名、存储实现 |
| 2 | Architecture spine + accepted ADR | 状态所有权、模块边界、依赖方向、运行与数据不变量 | 静默改变产品行为 |
| 3 | Active plan / story context | 本次改动范围、步骤、验证与风险 | 覆盖产品或架构契约 |
| 4 | 代码、迁移、schema、测试与 eval | 可执行实现和验证证据 | 单独创造未记录的新契约 |
| 5 | `handoff.md` | 当前 HEAD、正在进行的工作、证据、风险和唯一下一步 | 修改任何稳定决策 |
| 6 | research / archive / memlog | 决策输入与历史追溯 | 作为实现入口 |

`AGENTS.md` 位于上述层级之外：它只负责告诉协作者按什么顺序找到这些资产、如何安全工作。它不应复制完整产品或架构内容。

### 3.1 长期目录归属建议

BMAD 可以继续生成和协助维护材料，但长期权威不应永久绑定在工具输出目录中。建议在架构获批后，将稳定契约逐步提升到 `docs/`：

```text
docs/
  README.md                     # 权威地图和阅读顺序
  product/
    SPEC.md
    persona-contract.md
    conversation-policy.md
    memory-contract.md
  architecture/
    ARCHITECTURE.md             # 精简 spine
    decisions/                  # 只放已接受且仍生效的 ADR
  plans/
    active/
    completed/
    tech-debt.md
  quality/
    evaluation.md
    test-strategy.md
    datasets.md
  operations/
    observability.md
    security.md
    runbooks/
  research/                     # 有日期、非权威
  archive/                      # 只作历史追溯
```

`_bmad-output/` 随后只承担生成中产物、评审记录和研究输入。迁移完成前，现有 SPEC 路径继续是产品权威，避免出现两个同时有效的副本。

## 4. 推荐的仓库结构

首版保持一个 Maven project 和标准 `src/` 布局，减少自定义构建约定。这里的 `src/` 指重建后的新实现，不代表继承当前文件。

```text
/
  AGENTS.md
  README.md
  handoff.md
  docs/                         # 稳定知识与计划
  evals/
    datasets/public/            # 合成、脱敏、可提交用例
    schemas/                    # case/result/trace schema
    rubrics/                    # 评分标准与 CAP 映射
    README.md
  src/
    main/java/                  # capability-oriented production code
    main/resources/
    test/java/                  # fast deterministic tests
    test/resources/
  tools/                        # 文档、架构、eval 等仓库检查
  .github/workflows/
  mvnw
  mvnw.cmd
  .mvn/
  pom.xml
```

不建议现在建立多个 Maven modules。模块化首先由 package boundary、可见性和 ArchUnit 类结构测试落实；只有出现独立发布、不同生命周期、显著构建隔离或明确团队所有权后，才考虑物理拆分。

## 5. Architecture contract 应冻结的内容

Spine 不应成为冗长设计书。它应只冻结多个实现单元必须共同遵守、并能被测试或评审验证的不变量。

### 5.1 建议的能力边界

命名空间在 spine 阶段决定；以下名称只表达职责：

```text
conversation   回合编排、唯一主对话、上下文组装、最终表达
identity       稳定用户身份与可变用户资料
persona        版本化、只读的人格卡加载
memory         派生记忆的生成、检索、来源和冲突处理
search         唯一外部工具链、查询、证据、失败与披露
model          模型与 AgentScope 适配，不拥有产品状态
interface      HTTP/SSE 输入输出与边界校验
platform       配置、数据库、时钟、可观测性和安全基础设施
```

每个 capability 内可使用 `domain / application / port / adapter` 等局部分层，但不建立覆盖全仓库的 `controller/service/repository` 技术分层。跨 capability 调用只能经过公开 application contract 或 port；adapter 不能成为共享业务层。

### 5.2 状态所有权

| 状态 | 权威所有者 | 规则 |
| :-- | :-- | :-- |
| 原始对话账本 | conversation | 持久、可恢复；是用户真实表达的来源，不被摘要替代 |
| 用户身份 | identity | stable user ID 与 mutable profile 分离 |
| 人格卡 | persona | 版本化、只读；人格变化必须显式升级资产 |
| 派生记忆 | memory | 必须带 provenance，可失效/重建，不能伪装成原话 |
| 搜索证据 | search | 保存查询、来源、时间和失败；外部内容始终视为不可信数据 |
| 模型上下文 | conversation | 每回合按契约确定性组装；不是新的事实存储 |
| 框架 checkpoint/session | model/platform adapter | 仅为运行机制，不定义主对话、身份或记忆语义 |

conversation application 是一次回合的唯一编排者，也是最终输出策略的所有者。Memory、Search、Persona 和 Model 提供能力与证据，但不能各自拼装一份最终回复。

### 5.3 首个垂直切片

Spine 应选一个最小但完整的切片，而不是先创建空接口。建议首切片覆盖：

1. stable user 向唯一永久主对话发送文本；
2. 回合和消息先可靠写入真实 PostgreSQL；
3. 从近期原始历史进行最小召回；
4. 加载一张版本化 Alice persona card；
5. 通过模型 adapter 流式输出；
6. 保存完成、失败或中断状态；
7. trace 能关联一次请求、一次回合、持久化和模型调用；
8. 一个 CAP-1/CAP-2/CAP-5 evaluation 可以重放并得到报告。

真实搜索作为下一条垂直扩展加入同一个编排链，不先建设通用工具平台。

### 5.4 Spine 必须显式 deferred

- 具体长期记忆算法、向量库或知识图谱；
- 多人格管理和运行时切换；
- 多租户、权限平台和高并发扩展；
- 通用 tool/plugin runtime；
- 图片输入输出；
- 多 agent 产品运行时；
- 自动偏好更新、自然语言纠错与撤回。

## 6. Evaluation 与回归策略

测试验证代码机制，evaluation 验证开放式 AI 行为；二者缺一不可。

### 6.1 评测资产

每个 eval case 至少包含：

- case ID、关联 CAP、风险等级和版本；
- 输入消息、必要的初始数据库状态和确定性依赖；
- 期望的硬约束；
- 允许多种正确表达的 rubric；
- 需要检查的 trace/工具轨迹；
- 隐私等级与是否允许入库。

公开目录只保存合成或脱敏数据。来自真实对话的 private set 保持 gitignored；仓库只保存 schema、case hash、汇总指标和可复现的运行配置，不能把用户生活内容带入 Git、日志或第三方评测服务。

### 6.2 分层回归

| 层级 | 每次 PR | 定期/发布前 | 主要防护 |
| :-- | :--: | :--: | :-- |
| Domain/unit tests | 是 | 是 | 状态机、排序、失败语义、纯规则 |
| Architecture tests | 是 | 是 | 依赖方向、adapter 隔离、禁用边界 |
| Contract/API tests | 是 | 是 | HTTP/SSE、error、schema 兼容性 |
| PostgreSQL integration | 是 | 是 | migration、事务、重启恢复、并发边界 |
| Deterministic agent tests | 是 | 是 | fake model/search 下的编排和 trace |
| Offline behavior eval smoke | 是 | 是 | CAP 红线与高风险场景 |
| Live provider eval | 否 | 是 | 真实模型质量、成本和供应商漂移 |
| Human review | 高风险变更 | 是 | 人格、关系边界、过度迎合等细腻质量 |

关键行为不能只用 exact string 比较，也不能只依赖 LLM-as-judge。应组合：

- hard assertions：是否提问、是否伪造搜索/记忆、是否泄露来源或隐私；
- trace assertions：是否触发搜索、引用何种记忆、工具是否越权；
- rubric scoring：具体回应、Alice 人格、问题可回答性、记忆钩子；
- 人工抽检：高影响人格和关系安全场景。

优先建立的 regression suites：CAP-1 接住、CAP-2 永久主对话、CAP-5 重启召回/诚实 no-hit、CAP-6 搜索触发与失败、混合消息顺序、歧义分级、一次性温和异议、搜索与记忆冲突披露。

## 7. Testing、CI 与质量门禁

### 7.1 可复现开发环境

- 提交 Maven Wrapper，并校验下载 checksum。
- 用 Maven Enforcer 或 Toolchains 明确 Java 21，确保 compiler、Surefire 和 Javadoc 使用同一 JDK。
- 提供单一的快速入口，例如 `./mvnw verify`；CI 与本地使用相同入口。
- 用 Testcontainers 验证真实 PostgreSQL，不用 H2 compatibility mode 代替发布门禁。

### 7.2 建议的 PR 门禁

1. 格式化、编译、静态分析和 forbidden API 检查；
2. unit + architecture + contract tests；
3. PostgreSQL integration 与 migration validation；
4. offline eval smoke；
5. 文档链接、authority metadata 和 handoff schema 检查；
6. secret scanning、dependency review 和 code scanning；
7. 生成测试/eval/coverage 摘要作为 review 证据。

不建议一开始把不稳定且昂贵的 live model eval 设成每个 PR 的硬门禁。它应在固定模型、固定参数、固定预算下定期运行；一旦稳定，再按风险选择是否升级为 release gate。

## 8. 可观测性与运行安全

### 8.1 OpenTelemetry-first

一次用户回合应产生一条可关联的 trace，至少覆盖：

- request 与 turn lifecycle；
- identity/persona resolution；
- context assembly 与 memory recall；
- search decision、query、result/failure；
- model invocation、首 token、流式完成/中断；
- conversation/memory persistence。

指标至少包括 TTFT、总延迟、失败率、token/成本、搜索触发/成功/无结果、memory hit/no-hit/conflict、未完成回合和持久化失败。结构化日志包含 trace ID 与稳定错误码。

默认只采集元数据。prompt、completion、memory、搜索正文和用户资料默认不进入 telemetry；需要内容级调试时必须显式开启、脱敏、限时并可审计。

### 8.2 安全边界

- 搜索结果、网页文本和模型输出都是不可信输入，不能成为系统指令。
- 首版 search tool 只暴露最小、只读、schema 化的能力；对输入、输出、大小、超时和域策略做校验。
- 模型不能直接访问数据库、文件系统、网络或 secrets。
- secrets 只通过本地 ignored 配置或运行环境注入，禁止进入 prompt、trace、日志和 eval artifact。
- memory 写入必须记录来源和生成版本；冲突不能被静默覆盖。
- 建立 prompt injection、敏感信息泄露、过度代理权、资源耗尽和供应链攻击的回归场景。
- 备份、恢复和 migration rollback 必须有可执行 runbook，并定期演练。

## 9. Agent Development Workflow

建议把 agent 协作设计成可审计的 artifact pipeline：

```text
产品/缺陷输入
  -> contract impact check
  -> active execution plan
  -> isolated implementation
  -> builder evidence
  -> independent review
  -> tests + eval report
  -> human decision
  -> handoff / completed plan / debt ledger
```

### 9.1 角色规则

- Planner 只拆范围、契约影响、验收和风险，不预写实现结论。
- Builder 只读取当前 SPEC、spine 与 story context；需要改变契约时停止并升级，而不是在代码里偷偷决定。
- Reviewer 读取真实 diff、命令结果和 eval report，不采信 builder 自述。
- 多 agent 并行只用于文件所有权和验收边界清晰的独立子任务；禁止多个 agent 同时修改同一权威文件或同一实现边界。
- 一次变更必须有唯一 integration owner；失败、超时和未完成工作进入 handoff，而不是靠聊天记忆延续。

### 9.2 Handoff schema

`handoff.md` 应保持短且可验证，只记录：

- 日期、branch、HEAD 和工作区状态；
- 当前阶段与适用的权威文档版本；
- 本轮实际改动；
- 执行过的精确验证命令与结果；
- 已知风险/未决问题；
- 唯一下一步与禁止事项。

能从 Git 或测试结果自动获得的字段应由检查脚本验证，减少陈旧手写状态。

## 10. Technical Debt 与依赖管理

技术债使用一个可检索 ledger，而不是散落 TODO。每项至少记录 ID、影响、证据、处理触发条件、负责人/复查日期和状态。只有能在局部解决且不改变契约的 TODO 才允许留在代码中，并引用 debt ID。

依赖策略：

- 版本集中在 Maven properties/dependency management；只声明实际使用的直接依赖。
- 更新机器人按生态或风险分组，常规更新设置稳定观察期，安全更新优先。
- 框架、模型 SDK、AgentScope 或数据库大版本升级必须做 compatibility spike，比较 contract tests 与 eval baseline；产生架构影响时写 ADR。
- CI 检查新增依赖的许可、已知漏洞和依赖树变化；发布阶段生成 SBOM 和 provenance。
- 定期删除未使用依赖、废弃 feature flag 和无法触发的兼容代码。

## 11. 分阶段执行计划

### R0 — Freeze and evidence（本轮完成）

- 冻结业务开发；
- 盘点权威、历史、Git 变化和 legacy 实现；
- 在 JDK 21 下建立现有测试基线；
- 形成非权威重新初始化提案。

退出证据：本提案、更新后的 handoff、`25 tests / 0 failures` 基线。

### R1 — Authority normalization（仅文档）

- 用户评审并裁决本提案；
- 明确长期权威层级和稳定文档位置；
- 给每个权威文档增加 status、owner、last reviewed、supersedes/companions 等最小 metadata；
- 修正 `AGENTS.md`、文档索引与 handoff，使其只承担各自职责。

退出条件：不存在两个同级且内容冲突的产品或技术权威。

### R2 — Architecture spine

- 冻结状态所有权、依赖方向、数据/失败不变量和首个垂直切片；
- 明确 AgentScope、模型、搜索和 PostgreSQL 的 adapter 边界；
- 定义可由 ArchUnit/integration tests 执行的规则；
- 列出 deferred 与未来拆分触发条件。

退出条件：两个独立 builder 能从同一 spine 得出兼容的实现边界。

### R3 — Engineering harness（仍不开发业务功能）

- Maven Wrapper + Java 21 enforcement；
- CI、真实 PostgreSQL test infrastructure、ArchUnit；
- evaluation schema/runner/smoke dataset；
- observability、安全和本地运行约定；
- 文档与 handoff 检查。

退出条件：空的重建骨架能在本地与 CI 中用同一命令通过所有初始化门禁。

### R4 — First vertical slice

- 归档快照后移除 legacy implementation；
- 只实现 spine 定义的第一个端到端切片；
- 由独立 reviewer 按 SPEC、spine、tests 和 eval 验收。

### R5 — Search and hardening

- 在同一 conversation orchestration 中加入唯一真实搜索链路；
- 增加 injection、失败、冲突披露和成本回归；
- 完成备份恢复、观测 dashboard 和 release gates。

## 12. 对现有资料的处理提案（本轮不执行）

### 12.1 当前 `src/` 与测试

建议在 R4 开始前创建可恢复的 Git tag/branch，然后从默认分支删除 legacy `src/`，再按新 spine 重建。不要长期把旧实现复制到 `legacy/` 子目录；它会持续进入搜索、IDE 索引和 agent 上下文，造成错误模仿。

影响：默认分支会失去旧代码的直接文件路径，但 Git 历史完整保留；有价值的行为应先转写成与新契约对应的 tests/eval cases，而不是保留旧接口。

### 12.2 `docs/archive/`

短期保留且默认排除。长期可只在主分支保留一份历史清单和链接，将全文留在 archive tag/branch。

影响：减少 agent 上下文噪声，但历史追溯需要切换 Git ref。是否执行应由用户单独裁决。

### 12.3 `_bmad-output/`

在 R1 完成单次、可审阅的“提升”迁移：稳定契约进入 `docs/`，BMAD output 保留生成中材料。迁移不能复制后双轨维护；旧路径应改为指向新权威的短说明或在同一提交删除。

影响：所有 agent 指令、索引和 workflow 文档必须在同一次变更中更新；否则会制造新的权威分叉。

### 12.4 当前 architecture spine 模板

在它被真正填写并通过评审之前，应明确标为 template/non-authoritative，不能因为文件名存在就被工具当成技术权威。

## 13. 需要用户裁决的初始化决策

进入 R1 前只需裁决三件事：

1. 是否接受“稳定契约最终进入 `docs/`，`_bmad-output/` 只作生成/研究区”的长期模型；
2. 是否接受“单 Maven 模块的 capability-oriented modular monolith”作为 spine 的默认候选；
3. 是否接受在 R4 前做可恢复快照后，从默认分支移除 legacy `src/`，而不是在主树保留 legacy 副本。

以上任何一项未获确认，都不妨碍继续写 architecture candidate，但会影响 R1 的目录迁移和 R4 的清理策略。

## 14. 一手资料索引

- OpenAI, [Harness engineering](https://openai.com/index/harness-engineering/)
- OpenAI, [Agent evals](https://developers.openai.com/api/docs/guides/agent-evals)
- Anthropic, [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)
- Google, [Agents CLI project structure](https://google.github.io/agents-cli/guide/project-structure/)
- Google ADK, [Evaluate agents](https://github.com/google/adk-docs/blob/main/docs/evaluate/index.md)
- AWS, [Agentic AI Lens best practices](https://docs.aws.amazon.com/wellarchitected/latest/agentic-ai-lens/appendix-a.html)
- Microsoft, [Agent Framework workflow observability](https://learn.microsoft.com/en-us/agent-framework/workflows/observability)
- AgentScope Java, [Harness architecture](https://github.com/agentscope-ai/agentscope-java/blob/main/docs/v2/en/docs/harness/architecture.md)
- CloudWeGo Eino, [Official repository guide](https://github.com/cloudwego/eino/blob/main/llms.txt)
- ReAct, [Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629)
- MetaGPT, [Meta Programming for Multi-Agent Collaborative Framework](https://arxiv.org/abs/2308.00352)
- ChatDev, [Communicative Agents for Software Development](https://arxiv.org/abs/2307.07924)
- SWE-agent, [Agent-Computer Interfaces Enable Automated Software Engineering](https://arxiv.org/abs/2405.15793)
- MAST, [Why Do Multi-Agent LLM Systems Fail?](https://arxiv.org/abs/2503.13657)
- OWASP, [Top 10 for Large Language Model Applications](https://genai.owasp.org/llm-top-10/)
- OpenTelemetry, [Semantic conventions for generative AI spans](https://github.com/open-telemetry/semantic-conventions-genai/blob/main/docs/gen-ai/gen-ai-spans.md)
- Apache Maven, [Maven Wrapper](https://maven.apache.org/tools/wrapper/)
- Apache Maven, [Toolchains Plugin](https://maven.apache.org/plugins/maven-toolchains-plugin/)
- GitHub, [Secret scanning](https://docs.github.com/en/code-security/concepts/secret-security/secret-scanning)
- GitHub, [Dependency review](https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/manage-your-dependency-security/configure-dependency-review-action)

---

本文是 research/proposal。获批的稳定结论必须进入产品契约、architecture spine、质量策略或运行手册后，才能成为实现依据。
