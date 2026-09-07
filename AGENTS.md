# AGENTS.md · OpenAlice（A.L.I.C.E. 爱丽丝）

> 本文件给所有在本仓库工作的 Codex / 协作者作为入口指引；运营细节、红线与交接进度以 `handoff.md`（仓库根目录）为准。
> 当前阶段：**phase1-implementation** —— 单模块分层骨架与最小可运行链路已跑通（`mvn clean test` 全绿）。

## 目录结构

```text
OpenAlice/
├── README.md
├── AGENTS.md
├── handoff.md
├── pom.xml                 # 单模块 Spring Boot 应用工程（唯一可运行 jar）
├── src/
│   ├── main/java/openalice/
│   │   ├── OpenAliceApplication.java   # 启动类（组合根 = Spring 容器）
│   │   ├── model/                      # 纯 POJO：ChatMessage / UserId / SessionId
│   │   ├── enums/                      # 共享枚举：MessageRole / LlmProvider（零依赖）
│   │   ├── port/                       # 端口接口：MemoryPort（= dao 抽象层）
│   │   ├── memory/                     # MemoryPort 的内存实现（将来换 PostgreSQL 实现）
│   │   ├── agent/
│   │   │   ├── runtime/                # AgentRuntime 接口 + AgentScope 实现 + 工厂 + 配置
│   │   │   └── llm/                    # 模型接入：LlmModelFactory / mock / HTTP 传输
│   │   ├── service/                    # ★ ChatService：一次 /chat 的业务编排（见下）
│   │   ├── controller/                 # HTTP 入口：ChatController / HealthController / 异常处理
│   │   ├── dto/                        # 出入参对象：ChatRequest / ChatResponse / MessageView
│   │   └── config/                     # Spring @Configuration：组装 MemoryPort 与 AgentRuntime
│   ├── test/java/openalice/            # 测试镜像 main 的包结构
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
3. 按任务需要挑读：ADR 08（当前单模块布局）、ADR 07（P1 底座决策）、`docs/CONTEXT.md`

## 文档权威层级

- 需求以《项目需求说明书》为核心参考；技术选型 / 架构结论以 `docs/decisions/` 最新决策为准。
- **ADR 08 修订 ADR 06**：Maven 四模块收敛为单模块 + 顶层包分层；ADR 05 的模块边界精神、开发规范、测试策略与扩展预留仍有效。
- 参考项目与需求书只作参考，不约束最终实现。

## 当前工程规则

- Java 21，**单 Maven 模块**，根 `pom.xml` 即 Spring Boot 应用（`openalice`）。
- AgentScope Java 2.0.2 是预选底座；Spring Boot 3.5.16 仅作 Web 壳。
- 不引入 Spring AI / Spring AI Alibaba。
- 包依赖单向（防乱）：`model ← port ← memory`、`model ← agent`、`service` 见 `port + agent + model`、`controller → service`、`config` 组装 `memory + agent`。
- `agent` 不依赖 `memory`，只依赖 `port.MemoryPort` 与 `model`；**编排在 `service.ChatService`，`agent.runtime` 只负责「问一次模型」**。
- `web/` 不进入 Maven Reactor。
- Phase 1 使用 deterministic model 与 in-memory memory，不接外部 LLM API（除非配了 `OPENALICE_*` 环境变量）。
- 语音、learning、插件、多 Agent 只保留架构位置，不提前创建空模块/空包。

## 开发规范

- 遵循 Spring 官方单模块建议与 Alibaba P3C / Google Java Style 中不冲突的部分。
- `model` 只放纯 POJO 与值对象，不允许出现框架注解或持久层依赖。
- `controller` 只做 HTTP 翻译，业务写在 `service`；换存储实现只动 `config`（组合根）。
- 测试分层：`model` 纯单元测试；`memory` 存储测试；`service` 用 fake `AgentRuntime` 测编排；`agent` 用默认 mock 模型测 runtime；`controller` 做 SpringBoot 集成测试。
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
mvn clean test          # 全量测试
mvn spring-boot:run     # 本地启动
mvn package             # 打包可执行 jar
```

## 沟通

- 中文沟通，技术名词可用英文。
- 产品面向唯一用户本人；AI 自称爱丽丝 / Alice。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
