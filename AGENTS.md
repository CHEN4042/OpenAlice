# AGENTS.md · OpenAlice（A.L.I.C.E. 爱丽丝）

> 本文件给所有在本仓库工作的 Codex / 协作者作为入口指引；运营细节、红线与交接进度以 `handoff.md`（仓库根目录）为准。
> 当前阶段：**phase1-implementation / P1.5 语义整理 + ADR 10 业务包结构** —— SSE 流式与 Turn / Context / Session 编排链路已跑通；对话业务收敛到 `chat` 业务包。

## 目录结构

```text
OpenAlice/
├── README.md
├── AGENTS.md
├── handoff.md
├── pom.xml                 # 单模块 Spring Boot 应用工程（唯一可运行 jar）
├── src/
│   ├── main/java/com/openalice/
│   │   ├── OpenAliceApplication.java   # 启动类（组合根 = Spring 容器）
│   │   ├── model/                      # ChatMessage / UserId / SessionId / ConversationTurn / TurnStatus / MessageRole
│   │   ├── chat/                       # ★ 业务：对话（业务包，包内按类型整理）
│   │   │   ├── service/                # ChatService · ContextAssembler · SessionCoordinator
│   │   │   └── store/                  # ConversationStore 接口 · memory/ 内存实现（将来 postgres/ 同级）
│   │   ├── agent/
│   │   │   ├── AgentRequest.java       # 显式 agent 输入：Turn + 最近上下文 + system prompt
│   │   │   ├── AgentEvent.java         # sealed event
│   │   │   ├── TextDeltaEvent.java · DoneEvent.java · ErrorEvent.java
│   │   │   ├── runtime/                # AgentRuntime / AgentScope 适配器 / factory / properties
│   │   │   └── llm/                    # LlmProvider / factory / mock / HTTP transport
│   │   ├── controller/                 # ChatController / HealthController / 异常处理
│   │   ├── dto/                        # ChatRequest / ChatStreamEvent / MessageView
│   │   └── config/                     # Spring @Configuration：组装存储与 agent
│   ├── test/java/com/openalice/        # 测试镜像 main 的包结构
│   └── main/resources/application.yml
├── web/                  # 前端占位，不进入 Maven
└── docs/
    ├── index.md
    ├── 项目需求说明书 v1.2.md
    ├── CONTEXT.md
    ├── 代码学习导览 v0.1.md
    └── decisions/
```

## 阅读顺序（每次开工先读）

1. `handoff.md`
2. `docs/index.md`
3. 按任务需要挑读：ADR 10（当前包结构与演进规则）、ADR 09（P1.5 语义）、ADR 07（P1 底座决策）、`docs/CONTEXT.md`

## 文档权威层级

- 需求以《项目需求说明书》为核心参考；技术选型 / 架构结论以 `docs/decisions/` 最新决策为准。
- **ADR 10 修订 ADR 09 的顶层布局**：业务包自治 + 包内按类型整理 + 演进触发规则；`repository/` → `chat/store/`、`service/` → `chat/service/`。
- **ADR 09 修订 ADR 08**：根包改为 `com.openalice`，`port + memory` 收敛为 `repository`，并引入 Turn / AgentEvent / ContextAssembler / SessionCoordinator（语义结论仍有效）。
- ADR 08 的“单 Maven 模块 + service 编排”核心结论仍有效；ADR 05 的边界精神、开发规范与测试策略仍有效。
- 参考项目与需求书只作参考，不约束最终实现。

## 当前工程规则

- Java 21，**单 Maven 模块**，根 `pom.xml` 即 Spring Boot 应用（`io.openalice:openalice`）。
- AgentScope Java 2.0.2 是预选底座；Spring Boot 3.5.16 仅作 Web 壳。
- 不引入 Spring AI / Spring AI Alibaba。
- 包依赖单向：`model ← chat.store`、`model ← agent`、`chat.service → model + chat.store + agent`、`controller → chat.service + dto`、`config` 组装 chat.store 与 agent.runtime。
- 包结构遵循 ADR 10：默认留在本地、跨业务共享才上提 `model/`；不建全局 `enums/`、不提前建空业务包。
- `ConversationStore` 是业务会话历史唯一真相源；AgentScope state store 只是运行态 scratch，每次调用前清空。
- `ChatService` 负责 Turn 生命周期与消息持久化；`ContextAssembler` 负责显式上下文；`AgentRuntime` 只执行模型调用；`ChatController` 只做 HTTP/SSE 翻译。
- API 面向单用户：外部请求不携带 `userId`，服务端固定 `UserId.DEFAULT`；存储层保留 `userId` 字段。
- `web/` 不进入 Maven Reactor。
- 语音、learning、插件、多 Agent 只保留架构位置，不提前创建空模块/空包。

## 开发规范

- 遵循 Spring 官方单模块建议与 Alibaba P3C / Google Java Style 中不冲突的部分。
- `model` 只放纯 POJO 与值对象，不允许出现框架注解或持久层依赖。
- `controller` 只做 HTTP/SSE 翻译，业务写在 `chat.service`；换存储实现只动 `config`（组合根）。
- 测试分层：`model` 纯单元测试；`chat.store.memory` 存储测试；`chat.service` 用 fake `AgentRuntime` 测编排；`agent` 用默认 mock 模型测 runtime；`controller` 做 SpringBoot 集成测试。
- Agent 测试必须覆盖不同 session 的上下文隔离。
- 公共 API 与核心端口变更需同步更新 ADR / handoff / 本文件目录树 /《代码学习导览》。

## 公开安全

- 仓库按 public 安全标准维护。
- 禁止提交组织名称、内部项目细节、个人邮箱、本机绝对路径、用户名、SSH 私钥、token。
- 禁止提交 `.idea/`、`.DS_Store`、日志、构建产物。

## Git 协作规范

- **commit：AI 负责本地提交**，但必须等用户明确指示或 review 确认后执行。
- **push：一律由用户手动执行**。Codex 不执行 `git push`。
- message 遵循 Conventional Commits：`<type>(<scope>): <中文 subject>`。

## 常用命令

```bash
mvn clean test          # 全量测试（需 Java 21）
mvn spring-boot:run     # 本地启动
mvn package             # 打包可执行 jar
```

## 沟通

- 中文沟通，技术名词可用英文。
- 产品面向唯一用户本人；AI 自称爱丽丝 / Alice。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
