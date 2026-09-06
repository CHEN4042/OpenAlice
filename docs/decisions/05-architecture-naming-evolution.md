# 05 · OpenAlice 架构蓝图（讨论稿 v4）

> **状态说明（2026-09-06）**：本文的模块边界、开发规范、测试策略与扩展预留仍具参考价值；但 v4 中“使用 `service/` 聚合 Java 后端”的顶层布局**已被 [ADR 06](./06-phase1-root-module-layout.md) 修订**。当前 Phase 1 采用仓库根目录四模块：`openalice-core / openalice-memory / openalice-agent / openalice-server`，另保留 `web/` 前端占位。

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-06 |
| 状态 | 🟡 讨论中，尚未实施 |
| 目的 | 确定 OpenAlice 的仓库形态、后端模块边界、模块内部分层、开发规范、测试策略、运行时形态与后续扩展位置 |
| 关联 | [01 · AgentScope Java 2.0 vs Spring AI 2.0 选型分析](./01-agentscope-vs-springai.md) · [02 · MelonPaw 参考评估](./02-melonpaw-reference.md) |
| 当前倾向 | 顶层使用 `service/` 聚合 Java 后端，内部拆 `core / memory / agent / server` 四个 Maven 模块；`web/` 作为未来前端占位；语音、学习、插件、多 Agent 等能力先通过契约与包位置预留，不提前创建空 Maven 模块 |

---

## 0. 文档定位与参考资料边界

本节是 v3 的重要前提，避免后续开发被早期参考文档带偏。

1. 《项目需求说明书》、现有单模块参考项目、OpenJarvis、MelonPaw、Skylark 等材料在 OpenAlice 中的定位是**参考输入**，不是最终实现规范。
2. 这些参考中只有以下内容具有延续性：
   - OpenAlice 的主题与产品中心思想；
   - “记忆是灵魂”的核心定位；
   - M1–M3 自研记忆方向；
   - AgentScope Java 2.0 作为预选底座；
   - 两台电脑开发必须维护 handoff。
3. 参考材料中的系统架构、模块划分、功能边界、里程碑、话术、旧命名解释等内容**均不直接约束后续开发**。
4. 架构实施以本 ADR 的最终确认版本为准；与《项目需求说明书》冲突时，需求书需要后续回写。
5. **最终完整架构确认之前，不创建 Maven module、不创建代码目录、不启动 Phase 1 编码。**
6. 本 v4 新增的语音、模型优化、OpenHanako 式能力，只作为**扩展能力与接口位置预留**，不是 Phase 1 实施承诺。
7. 本文中列出的未来文件名是**设计占位名**，用于检查边界是否可扩展；在架构确认前不得创建。
8. 语音链路参考 Skylark / Hume AI / OpenAI Realtime，模型优化参考 AgentScope training extension 与 OpenJarvis 思路，OpenHanako 只作为桌面助手能力地图参考；它们都不能直接替代 OpenAlice 的 ADR 决策。

---

## 1. 架构结论

OpenAlice 推荐采用「**顶层聚合 + 后端多模块 + 前端独立占位**」的仓库形态：

```text
OpenAlice/
├── .github/
├── docs/
│
├── service/
│   ├── pom.xml
│   ├── openalice-core/
│   ├── openalice-memory/
│   ├── openalice-agent/
│   └── openalice-server/
│
├── web/
│   └── README.md
│
├── AGENTS.md
├── README.md
└── handoff.md
```

核心判断：

1. **`service/` 解决顶层噪声问题**：Java 后端相关内容集中在一个顶层目录，避免根目录被多个 Maven 模块占满。
2. **Maven 模块解决边界问题**：`memory` 与 `agent` 是 OpenAlice 的长期核心能力，不能只靠 package 约束。
3. **不是微服务**：四个 Java 模块仍在同一个 Spring Boot JVM 中运行，不引入 RPC、消息队列或分布式部署复杂度。
4. **`server` 是组合根**：由 `openalice-server` 负责把 agent 与 memory 装配起来，避免 `agent` 直接绑定 memory 实现。
5. **`web/` 独立于 Java 构建**：前端未来只通过 HTTP / WS / SSE 调用后端，不进入 Maven Reactor。
6. **端口先行**：`MemoryPort`、`TracePort`、`ToolContext` 等稳定抽象放入 `openalice-core`，但只定义边界，不提前实现复杂功能。
7. **扩展通过端口预留**：语音、学习、插件等能力先依赖稳定契约，而不是让 `agent / server / memory` 彼此直连。
8. **先包后模块**：新能力先放入既有模块的一级包；当职责、变更原因、依赖集合明显不同且规模足够时，再拆独立 Maven 模块。

---

## 2. 规范与参考调研

### 2.1 Maven 多模块规范

参考：

- Maven 官方指南：Multiple Modules Project

结论：

- Maven Reactor 根据 `<module>` 声明与依赖关系计算构建顺序；
- `<module>` 的值是相对路径，不要求模块必须位于仓库根目录；
- Maven 不禁止使用 `service/` 这类聚合目录；
- 父 POM 可以放在 `service/pom.xml`，并声明四个子模块。

因此，「后端模块放在 `service/` 下」并不违反 Maven 规范。

### 2.2 Spring 多模块建议

参考：

- Spring 官方指南：Multi-Module Projects

结论：

- 应区分 **library module** 与 **application module**；
- library module 依赖应尽量窄，不应全部引入 Spring Boot 应用依赖；
- 只有 application module 应保留 Spring Boot executable jar 打包；
- library module 不应生成可独立运行的 fat jar。

对 OpenAlice 的映射：

| OpenAlice 模块 | Maven 类型 | 说明 |
| :--- | :--- | :--- |
| `openalice-core` | library | 领域模型、端口、通用抽象 |
| `openalice-memory` | library | 记忆领域与持久化适配 |
| `openalice-agent` | library | AgentScope Harness 装配与运行时 |
| `openalice-server` | application | 唯一 Spring Boot executable module |

### 2.3 阿里巴巴 Java 开发手册

参考：

- Alibaba P3C
- 《Java 开发手册（黄山版）》

OpenAlice 采纳的原则：

1. 架构设计必须明确系统边界、模块关系、依赖方向、输入输出与演化原则；
2. 分层之间默认上层依赖下层，不允许任意循环依赖；
3. 接口与实现分离，核心能力优先面向稳定抽象；
4. ArtifactId 采用「产品线名-模块名」；
5. Service / DAO 等分层不应变成万能筐；
6. 常量、异常、日志、并发工具必须有清晰归属，不允许为了省事全部塞进 common。

### 2.4 Google Java Style

参考：

- Google Java Style Guide

OpenAlice 采纳的原则：

1. 类名使用 UpperCamelCase；
2. 方法与变量使用 lowerCamelCase；
3. 常量使用 UPPER_SNAKE_CASE；
4. 测试类与被测类保持对应命名；
5. 代码格式以项目 formatter 配置为准，避免风格争论；
6. Google Style 不规定仓库顶层目录，因此不能作为 `service/` 是否合法的依据。

### 2.5 大型开源项目布局

大型项目常见两类布局。

**平台型 / 框架型项目：模块直接放根目录**

例如 Kafka、Elasticsearch 这类项目常见：

```text
clients/
server/
core/
streams/
connect/
storage/
tools/
docs/
```

优点是平台能力一目了然，适合多个可独立发布组件。

**应用型 / 产品型项目：先按运行形态聚合**

例如 AgentScope Java 官方仓库：

```text
agentscope-core/
agentscope-harness/
agentscope-extensions/
agentscope-examples/
agentscope-service/
```

其中 `agentscope-service/` 内部继续聚合：

```text
agentscope-service/
├── service-common/
├── service-gateway/
├── service-dataplane/
├── service-scheduler/
├── frontend/
└── pom.xml
```

结论：

- 大型项目没有唯一标准；
- OpenAlice 是个人长期产品，不是发布通用框架；
- 采用 `service/` 聚合后端更符合产品型仓库心智。

### 2.6 关于“大厂规范”的使用边界

本 ADR 只把**公开、可验证、与 Java 工程直接相关**的规范作为依据：

- Maven 官方多模块指南；
- Spring 官方多模块指南；
- Alibaba P3C / 《Java 开发手册》；
- Google Java Style。

不把以下内容当作自动规范：

- 任何公司内部规范；
- 无法公开验证的团队约定；
- 网络传闻式“大厂最佳实践”；
- 参考项目中的临时做法。

如果未来要引入字节跳动或其他公司的公开开源规范，必须先补充官方链接与适用范围，再更新 ADR，而不是直接混入当前规则。

---

## 3. AgentScope Java 2.0 官方建议

参考：

- AgentScope Java Quickstart
- Harness Architecture
- Going to Production
- Training Integration
- AgentScope Java 官方仓库

关键结论：

1. 推荐通过 `agentscope-harness` SDK 引入，不建议一开始 fork 框架；
2. `HarnessAgent` 负责工作区、长期记忆、会话持久化、子 agent、沙箱等能力；
3. Agent 调用之间应视为无状态；
4. Agent 可以作为单例服务多个用户与会话；
5. 每次调用应通过 `RuntimeContext` 携带 `(userId, sessionId)`；
6. 相同 session 串行执行；
7. 不同 session 可以并发；
8. 生产环境不应依赖本地 JSON 状态存储，应使用 Redis / JDBC / PostgreSQL 等分布式实现；
9. 模型 provider 是独立扩展模块，应按需引入；
10. 官方提供 training extension，可通过采样流量、收集 trace、计算 reward、周期提交训练形成闭环。

对 OpenAlice 的直接约束：

- `openalice-agent` 中的 Agent **允许单例**，但前提是框架状态模型满足隔离要求；
- 不允许把用户会话状态存在 Agent 实例字段；
- 每次请求必须构造 `RuntimeContext`；
- AgentScope 的 `AgentState` 不等于 OpenAlice 的 M1–M3 记忆系统；
- 生产态状态存储应走 `openalice-memory` 或持久化适配层；
- Spring Boot 只做协议壳，不承载 Agent 核心逻辑；
- Phase 1 必须做双 session 并发隔离冒烟测试。

---

## 4. 仓库形态

### 4.1 顶层目录职责

| 路径 | 职责 | 当前是否创建 |
| :--- | :--- | :--- |
| `docs/` | 需求、决策、术语、交接文档 | 已存在 |
| `service/` | Java 后端 Maven 聚合工程 | 待架构确认后创建 |
| `web/` | 未来前端占位 | 待架构确认后创建 |
| `.github/` | CI、Issue 模板、工作流 | 后续需要时创建 |
| `README.md` | 项目门面 | 已存在 |
| `AGENTS.md` | AI / 协作者入口 | 已存在 |
| `handoff.md` | 两台电脑开发交接 | 已存在 |

### 4.2 Maven 父 POM

`service/pom.xml`：

- `packaging=pom`；
- 统一管理 Java 17、Maven 插件、AgentScope 2.0.2、Spring Boot 3.5.16；
- 声明四个 module；
- 不包含业务代码。

模块顺序：

```xml
<modules>
    <module>openalice-core</module>
    <module>openalice-memory</module>
    <module>openalice-agent</module>
    <module>openalice-server</module>
</modules>
```

### 4.3 构建入口

后端唯一常规入口：

```bash
cd service
mvn clean test
mvn -pl openalice-server -am spring-boot:run
```

前端未来独立入口：

```bash
cd web
# 待前端技术栈确认后定义
```

约定：

- `service/` 与 `web/` 是两个独立构建入口；
- Java 构建不触发前端构建；
- 前端构建不依赖 Maven；
- 根目录不放置统一 mega-build 脚本，除非未来确实需要。

---

## 5. 模块内部一级结构草案

本节只用于检查功能归属，**当前不创建目录**。最终 Java 根包在 Phase 1 初始化时另行确认；下文使用 `openalice.*` 仅为了表达结构。

### 5.1 `openalice-core`

```text
openalice-core/
└── src/main/java/openalice/core/
    ├── domain/       # 稳定领域模型与值对象
    ├── port/         # 跨模块稳定接口
    └── event/        # 领域事件与事件契约
```

建议的 `port` 一级内容：

```text
port/
├── MemoryPort.java             # M1–M3 记忆读写入口
├── TracePort.java              # trace 预留端口，Phase 1 可为空实现
├── ToolContext.java            # 工具执行上下文，隔离具体 memory 实现
├── ToolResult.java             # 工具返回契约
├── SpeechRecognitionPort.java  # 语音预留：音频流 / 片段 → 文本事件
├── TextToSpeechPort.java       # 语音预留：文本 → 音频流 / 音频事件
├── VoiceSessionPort.java       # 语音预留：语音会话编排稳定入口
└── ModelEvaluationPort.java    # 学习预留：评测 / 优化任务与 agent 运行时之间的隔离契约
```

`domain` 不按数据库表设计，而按业务概念设计：

```text
domain/
├── user/
├── session/
├── message/
├── memory/
├── persona/
├── audio/       # 语音预留：AudioChunk、Transcript、VoiceSession 等纯模型
└── learning/    # 学习预留：TraceRecord、DatasetItem、ModelVersion 等纯模型
```

核心约束：

- `core` 不出现 `impl` 包；
- `core` 不出现 Spring、AgentScope、数据库、Redis、LLM / ASR / TTS / RTC / 训练 SDK 依赖；
- `core` 只放稳定抽象，不放随手共享的工具类；
- 语音与学习相关接口只描述契约，不承载供应商逻辑、音频解码、模型训练实现；
- 如果 `core` 超过约 50 个类，必须触发一次拆分评审；
- 不预先创建 `common` 或 `shared` 包，确有稳定通用能力时再讨论。

### 5.2 `openalice-memory`

```text
openalice-memory/
└── src/main/java/openalice/memory/
    ├── domain/          # 记忆领域模型与状态
    ├── engine/          # M1–M3 记忆算法
    ├── store/           # 存储适配
    └── agentscope/      # AgentScope Memory 桥接
```

`engine` 内部建议：

```text
engine/
├── m1/    # 短期记忆
├── m2/    # 中期记忆
├── m3/    # 长期记忆
└── policy/ # 压缩、遗忘、冲突消解、评估策略
```

`store` 内部建议：

```text
store/
├── inmemory/   # Phase 1 跑通链路用
├── postgres/   # 未来关系型存储
├── redis/      # 未来缓存或短期状态
└── vector/     # 未来 pgvector / 向量检索适配
```

核心约束：

- `engine` 优先面向 `MemoryPort` 与记忆领域模型，不被具体存储绑架；
- `store` 只做技术适配，不承载业务规则；
- `agentscope` 只做桥接，不替代自研 M1–M3；
- Phase 1 的 `InMemoryMemoryPort` 可以放在 `store/inmemory`，作为临时运行实现；
- Agent 模块测试不得为了复用它而直接依赖 `openalice-memory`。

### 5.3 `openalice-agent`

```text
openalice-agent/
└── src/main/java/openalice/agent/
    ├── runtime/      # AgentScope Harness 装配与执行
    ├── persona/      # 人格资源、System Prompt、参数化配置
    ├── tool/         # 工具注册、调用、权限
    ├── trace/        # trace 埋点适配
    └── config/       # Agent 模块配置装配
```

预留扩展包（当前不创建，只说明未来归属）：

```text
runtime/
├── AgentRegistry.java       # 多 Agent / 多人格运行时注册
└── SubAgentDispatcher.java  # 子 Agent 与委派调度

persona/
├── PersonaCardService.java  # 人格卡导入 / 导出 / 校验
└── PersonaTemplate.java     # 人格模板与参数化配置

tool/skill/
└── SkillBundleLoader.java   # 技能包加载与白名单校验
```

`runtime` 建议职责：

```text
runtime/
├── AgentRuntime.java          # 对 server 暴露的稳定入口
├── AgentRuntimeFactory.java   # AgentScope Agent 创建 / 获取
├── AgentExecutor.java         # 请求执行与事件流
└── SessionIsolationGuard.java # 并发与隔离保护
```

`tool` 建议职责：

```text
tool/
├── ToolRegistry.java
├── ToolInvoker.java
├── ToolPermission.java
└── internal/                 # 第一批内置工具
```

核心约束：

- 工具不直接依赖 `openalice-memory`；
- 工具需要读记忆时，通过 `ToolContext` 暴露的 `MemoryPort`；
- 人格第一阶段放在本模块，不单独拆 `openalice-persona`；
- trace 只做埋点与转发，不做学习闭环。

### 5.4 `openalice-server`

```text
openalice-server/
└── src/main/java/openalice/server/
    ├── api/           # HTTP API 与 DTO
    ├── stream/        # SSE / WebSocket / 语音协议入口
    ├── composition/   # 组合根：agent + memory + 未来 speech 装配
    ├── security/      # 鉴权与基础安全
    ├── config/        # Spring 配置
    └── health/        # 健康检查与诊断
```

`stream` 中预留语音协议入口：

```text
stream/voice/
├── VoiceWebSocketHandler.java  # 浏览器 / 客户端音频 WebSocket 入口
├── VoiceSessionController.java # 语音会话生命周期 HTTP / WS 控制接口
└── VoiceProtocolMapper.java    # 协议 DTO 与 core 音频模型的转换
```

说明：`server` 只承载协议接入、鉴权、限流和 DTO 映射；语音编排与 ASR / TTS / VAD 实现规模变大后拆入未来 `openalice-speech` 模块。

核心约束：

- `composition` 是唯一允许同时感知 agent 与 memory 实现的位置；
- Controller / Endpoint 不写业务规则；
- DTO 不直接逃逸到 `core / memory / agent`；
- 鉴权、限流、安全策略只做协议层治理；
- 健康检查可以观察依赖状态，但不触发核心业务流程。

### 5.5 `web/`

```text
web/
└── README.md
```

未来再决定：

- React / Vue / 其他框架；
- pnpm / npm / bun；
- TypeScript / JavaScript；
- 是否使用 monorepo 前端工具。

当前只确定：

- `web/` 不进入 Maven Reactor；
- 只通过 HTTP / WS / SSE 调用 `openalice-server`；
- `web/README.md` 必须写清独立构建命令；
- 在技术栈确认前，不写死 pnpm + TypeScript。

---

## 6. 功能与模块归属矩阵

| 功能 | 归属模块 | 一级位置 | 备注 |
| :--- | :--- | :--- | :--- |
| 用户 / 会话 / 消息基础模型 | `openalice-core` | `domain` | 稳定领域概念 |
| 人格基础模型 | `openalice-core` | `domain/persona` | 只放稳定契约 |
| `MemoryPort` | `openalice-core` | `port` | Phase 1 必须定义 |
| `TracePort` | `openalice-core` | `port` | Phase 1 预留空接口 |
| `ToolContext` | `openalice-core` | `port` | 工具访问记忆的唯一入口 |
| 领域事件 | `openalice-core` | `event` | 先定义契约，不引入消息系统 |
| M1 短期记忆 | `openalice-memory` | `engine/m1` | 自研核心 |
| M2 中期记忆 | `openalice-memory` | `engine/m2` | 自研核心 |
| M3 长期记忆 | `openalice-memory` | `engine/m3` | 自研核心 |
| 记忆压缩 / 遗忘 / 冲突消解 | `openalice-memory` | `engine/policy` | 算法优先，存储后置 |
| 内存版记忆实现 | `openalice-memory` | `store/inmemory` | Phase 1 跑通链路 |
| PostgreSQL / Redis / 向量存储 | `openalice-memory` | `store/*` | 后续替换 |
| AgentScope Memory 桥接 | `openalice-memory` | `agentscope` | 不替代 M1–M3 |
| AgentScope Harness 装配 | `openalice-agent` | `runtime` | SDK 引入，不 fork |
| Agent 执行入口 | `openalice-agent` | `runtime` | 对 server 暴露稳定 API |
| 会话隔离与并发保护 | `openalice-agent` | `runtime` | Phase 1 冒烟测试 |
| 人格 System Prompt | `openalice-agent` | `persona` | 第一阶段不拆模块 |
| 工具注册与调用 | `openalice-agent` | `tool` | 通过 ToolContext 访问能力 |
| 工具权限 | `openalice-agent` | `tool` | 默认拒绝，显式授权 |
| trace 埋点 | `openalice-agent` | `trace` | 转发 TracePort |
| HTTP API | `openalice-server` | `api` | 协议壳 |
| SSE / WebSocket | `openalice-server` | `stream` | 流式输出 |
| agent + memory 装配 | `openalice-server` | `composition` | 唯一组合根 |
| 鉴权与安全 | `openalice-server` | `security` | 协议层治理 |
| 健康检查 | `openalice-server` | `health` | 不触发业务流程 |
| 聊天 UI | `web/` | 未来 | 暂不实现 |
| 记忆管理 UI | `web/` | 未来 | 暂不实现 |
| 语音领域模型与端口 | `openalice-core` | `domain/audio` / `port` | 只预留契约，不放 SDK |
| 首版半双工语音入口 | `openalice-server` | `stream/voice` | 浏览器录音 → ASR → agent → TTS |
| 全双工语音 / 打断 / turn-taking | 未来 `openalice-speech` | `session / turntaking / asr / tts / vad` | 先预留位置，不提前建模块 |
| trace 数据集 / 评测 / 训练 | 未来 `openalice-learning` | 未来 | 当前只预留位置 |
| 线上强模型 → 本地模型优化 | 未来 `openalice-learning` | `teacher / training / evaluation / registry` | 参考 AgentScope training extension 与 OpenJarvis 思路 |
| 多 Agent / 子 Agent 委派 | `openalice-agent` | `runtime` 预留 | 不改变 agent 与 memory 的端口隔离 |
| 人格卡 / 人格模板导入导出 | `openalice-agent` | `persona` 预留 | 参考 OpenHanako 能力，不照搬实现 |
| 技能包 / 插件系统 | `openalice-agent/tool` → 未来 `openalice-plugin` | 预留 | 插件必须经权限与 ToolContext，不能直连 memory |
| 主动任务 / 心跳 / 定时调度 | 先 `openalice-server` / `agent`，后 `openalice-worker` | 预留 | 触发与执行分离 |
| 外部聊天渠道接入 | 先 `openalice-server`，后 `openalice-channel` | `channel` 预留 | 只做协议适配，不复制 agent 逻辑 |
| 附件 / 媒体 / 工作台文件 | 先 `core.domain.message`，后 `openalice-media` | 预留 | 统一 attachment 身份与权限 |

---

## 7. 依赖方向

推荐依赖：

```text
openalice-core
    ↑
openalice-memory

openalice-core
    ↑
openalice-agent

openalice-agent
    ↑
openalice-server

openalice-memory
    ↑
openalice-server
```

具体规则：

1. `openalice-core` 不依赖任何业务模块；
2. `openalice-memory` 依赖 `core`，实现 `MemoryPort`；
3. `openalice-agent` 依赖 `core`，只调用 `MemoryPort`，不直接依赖 `openalice-memory`；
4. `openalice-server` 依赖 `agent` 与 `memory`，作为组合根完成注入；
5. `web/` 不依赖 Java 模块，只通过协议调用 `server`；
6. `tools` 未来即使拆成独立模块，也只依赖 `core` 的 `ToolContext`，不直接依赖 `memory`。

关键调整：

> `openalice-agent` 不直接依赖 `openalice-memory`。
> 这样 agent 可以围绕稳定端口开发，memory 可以独立演进算法与存储，server 负责最终装配。

---

## 8. 运行时形态

四个 Java 模块最终仍在同一个 Spring Boot JVM 内：

```text
openalice-server.jar
├── openalice-server
├── openalice-agent
├── openalice-memory
└── openalice-core
```

因此：

- 不是微服务；
- 不需要 RPC；
- 不需要消息队列作为基础通信；
- 模块间是 JVM 内方法调用；
- Maven module 提供的是**编译期边界**，不是部署边界。

多模块不会自动导致每次重启都显著变慢：

- 只有 `openalice-server` 是 Spring Boot 应用；
- `core / memory / agent` 是 library module；
- IDE 增量编译与 Maven `-pl ... -am` 可以只构建受影响模块；
- Phase 1 不建议为了“调试方便”合并模块；
- 如果启动速度成为真实瓶颈，优先优化 Spring Bean 装配与自动配置，而不是取消模块边界。

---

## 9. 核心请求流

```text
web
  ↓ HTTP / WS / SSE
openalice-server
  ↓ 构造 RuntimeContext(userId, sessionId)
openalice-agent
  ↓ 调用 MemoryPort
openalice-core
  ↓ 接口注入
openalice-memory
  ↓ Phase 1: InMemoryMemoryPort；后续: PostgreSQL / Redis / pgvector
openalice-agent
  ↓ AgentScope Harness / Model / Tools
模型与外部工具
  ↓ AgentEvent
openalice-server
  ↓ SSE / WebSocket
web
```

工具调用记忆时的路径：

```text
openalice-agent/tool
  ↓ ToolContext
openalice-core/port/MemoryPort
  ↓ server 注入的具体实现
openalice-memory
```

这条路径保证未来拆出 `openalice-tools` 时，不会反向依赖 `openalice-memory`。

---

## 10. 开发规范

以下规范在 Phase 1 开始后必须执行。

### 10.1 通用工程规范

1. Java 版本固定为 17，不使用未确认的 preview feature。
2. Maven module 依赖必须单向，禁止循环依赖。
3. 模块间只能通过明确 API / port 交互，不允许反射绕过边界。
4. 优先构造器注入，不使用字段注入。
5. 配置项统一外部化，不硬编码密钥、路径、模型名。
6. 不提交 `.DS_Store`、`.idea/`、密钥、token、本地环境文件。
7. 类、方法、包名使用英文；文档与沟通使用中文。
8. 不创建“临时万能包”；`utils`、`common`、`helper` 必须说明稳定归属。
9. 公共 API 最小化，包级私有优先。
10. 领域对象优先不可变，避免随意 setter 污染状态。

### 10.2 `openalice-core` 防膨胀规范

1. `core` 只允许放稳定领域模型、端口、事件契约。
2. `core` 禁止出现 `impl` 包。
3. `core` 禁止依赖 Spring Boot、AgentScope、数据库、Redis、LLM SDK。
4. 两个模块共用的工具类不能默认放入 `core`；必须先证明它是稳定领域概念。
5. `core` 超过约 50 个类时，必须触发架构评审。
6. 单个包超过约 20 个类时，必须检查是否职责混杂。
7. 任何新 port 都需要回答：
   - 谁调用；
   - 谁实现；
   - 是否跨模块；
   - 是否真的稳定；
   - 是否可以用现有 port 表达。

### 10.3 命名规范

1. Maven artifactId：`openalice-<capability>`。
2. 类名 UpperCamelCase。
3. 方法与变量 lowerCamelCase。
4. 常量 UPPER_SNAKE_CASE。
5. 测试类：`被测类名 + Test`。
6. 接口名优先表达能力，如 `MemoryPort`，不使用 `IMemoryService`。
7. 实现类名表达技术选择，如 `InMemoryMemoryPort`、`PostgresMemoryStore`。
8. 包名表达职责，不复制完整 Maven 模块名造成重复。
9. 禁止使用无意义包名：`misc`、`temp`、`new`、`manager2`。
10. DTO 只存在于协议边界附近，不进入 core 领域层。

### 10.4 异常与日志规范

1. 不吞异常，不捕获 `Exception` 后空处理。
2. 业务异常使用明确类型，不用字符串魔法值表达错误。
3. 日志必须包含 `userId` / `sessionId` / `traceId` 等可定位字段。
4. 日志禁止输出密钥、完整 prompt 中的隐私内容、未脱敏记忆。
5. 外部调用日志要记录耗时、结果状态与技术错误。
6. 不使用 `System.out.println` 替代日志。
7. 单元测试中的预期异常用断言表达，不用日志人工判断。

### 10.5 配置与安全规范

1. 生产配置与本地配置分离。
2. 所有外部服务地址、key、模型参数走环境变量或安全配置。
3. 默认最小权限：工具默认不可用，显式授权后开放。
4. 沙箱与本地执行工具必须有权限边界。
5. Web 输入必须在 server 层校验，不直接映射为内部对象。
6. 不把数据库实体直接暴露给 API。
7. 记忆数据属于高敏数据，导出、日志、trace 必须考虑脱敏。

### 10.6 Git 与协作规范

1. 架构讨论期间不创建代码目录。
2. 未获用户明确指示，不 commit。
3. push 一律由用户手动执行。
4. 架构规则变更必须同步更新 ADR 与 handoff。
5. 每次会话结束更新 handoff。
6. 大改动先给结构草案，用户确认后再实施。
7. 代码落地后遵循 Conventional Commits。

---

## 11. 测试策略

### 11.1 测试分层

| 层级 | 目标 | 依赖要求 |
| :--- | :--- | :--- |
| `core` 单元测试 | 领域对象、端口契约 | 不依赖 Spring、AgentScope、数据库 |
| `memory` 算法测试 | M1–M3 写入、检索、遗忘、冲突消解 | 可用纯内存对象 |
| `memory` 存储测试 | adapter 行为一致 | 后续用 Testcontainers / embedded store |
| `agent` 单元测试 | Agent 编排、工具调用、权限 | 使用 fake `MemoryPort` / `TracePort` |
| `server` 集成测试 | HTTP / SSE / WS 全链路 | 可注入 `InMemoryMemoryPort` |
| 并发隔离测试 | 双 session 上下文不串话 | Phase 1 必做 |

### 11.2 模块测试依赖规则

1. `agent` 测试不得依赖 `openalice-memory`。
2. `agent` 测试使用自己编写的 fake `MemoryPort`。
3. `memory` 测试不得依赖 `agent` 或 `server`。
4. `server` 集成测试允许依赖全部模块，因为它是组合根。
5. Phase 1 不使用 Maven test-jar 共享测试代码。
6. 如果 fake 重复到明显浪费，再讨论独立的 `openalice-test-fixtures`，不提前创建。

### 11.3 Phase 1 测试顺序

1. `core`：`MemoryPort`、`ToolContext`、基础领域对象；
2. `memory`：`InMemoryMemoryPort` 行为测试；
3. `agent`：使用 fake memory 的执行测试；
4. `server`：HTTP / SSE 最小集成测试；
5. 并发隔离：两个 session 并行请求，断言记忆与上下文不串话。

### 11.4 常用测试命令

```bash
# 只测 core
cd service
mvn -pl openalice-core test

# 只测 memory
mvn -pl openalice-memory test

# 只测 agent
mvn -pl openalice-agent test

# 只测 server，同时构建依赖
mvn -pl openalice-server -am test

# 全量测试
mvn clean test
```

---

## 12. AgentScope 状态与并发守卫

这是 Phase 1 的第一优先级风险。

必须验证：

1. `HarnessAgent` 是否真的可以单例复用；
2. `RuntimeContext(userId, sessionId)` 是否足以隔离状态；
3. AgentScope 内部是否缓存会话状态；
4. 相同 session 是否串行；
5. 不同 session 并发时是否串话；
6. Agent 实例字段中是否出现请求级可变状态。

Phase 1 冒烟测试：

```text
session A: “我的代号是 Alpha”
session B: “我的代号是 Beta”

并行请求后：
session A 询问“我的代号是什么？” → 期望 Alpha
session B 询问“我的代号是什么？” → 期望 Beta
```

守卫规则：

- 如果 AgentScope 保证无状态，可以使用单例；
- 如果存在实例内状态，必须改为工厂创建或请求级执行单元；
- OpenAlice 自己的代码永远不把会话状态存进 Agent 字段；
- 会话状态归 memory / persistence 管，Agent 只做执行。

---

## 13. 扩展能力预留设计

### 13.1 预留原则

1. **不提前创建空 Maven 模块**：`openalice-speech / openalice-learning / openalice-plugin` 等只作为未来拆分方向。
2. **先定义稳定契约**：跨模块能力进入 `openalice-core.port`，实现可以后置。
3. **协议层与能力层分离**：`server.stream` 只做 WebSocket / HTTP / WebRTC 协议接入；语音编排、ASR / TTS / VAD 属于能力层。
4. **供应商可替换**：ASR / TTS / RTC / LLM SDK 不进入 `core`，只能进入对应能力模块或 server 适配层。
5. **学习闭环不得反向污染 agent**：agent 只写 `TracePort` / 领域事件，不依赖训练、数据集、评测实现。
6. **插件不得绕过权限**：插件能力通过 `ToolContext`、权限策略与受控事件访问系统，不允许直接拿 `MemoryPort` 实现或数据库连接。
7. **新能力先入既有包，规模与变更原因足够时再拆模块**，避免 Maven module 膨胀。

### 13.2 语音能力预留

#### 首版目标

首版只要求可用语音对话，不要求 ChatGPT Advanced Voice / Hume EVI 级全双工体验：

```text
web 点按录音
  ↓ WebSocket / HTTP
server.stream.voice
  ↓ SpeechRecognitionPort
文本输入
  ↓ AgentRuntime
AgentEvent / 文本输出
  ↓ TextToSpeechPort
音频流
  ↓ server.stream.voice
web 播放
```

这是**半双工或轻量双向**语音链路，优先验证会话隔离、延迟、失败降级与文本兜底。

#### 未来全双工形态

全双工需要连续音频流、实时 VAD、turn-taking、打断、回声消除和流式 ASR / TTS。架构预留如下：

```text
openalice-speech/                 # 未来模块，当前不创建
└── src/main/java/openalice/speech/
    ├── session/
    │   ├── VoiceSessionManager.java
    │   ├── VoiceSessionStateMachine.java
    │   └── DuplexSessionStateMachine.java
    ├── turntaking/
    │   ├── BargeInPolicy.java
    │   ├── BackchannelFilter.java
    │   └── TurnTakingPolicy.java
    ├── audio/
    │   ├── EchoCancellationPolicy.java
    │   └── AudioBuffer.java
    ├── asr/
    │   └── StreamingAsrAdapter.java
    ├── tts/
    │   └── StreamingTtsAdapter.java
    ├── vad/
    │   └── VoiceActivityDetector.java
    └── codec/
        └── AudioCodecAdapter.java
```

兼容规则：

- `openalice-agent` 不感知麦克风、扬声器、Opus / PCM 编码和 WebRTC 细节；
- 语音层将音频转换为 `Transcript` 或结构化事件后进入 `AgentRuntime`；
- TTS 消费 agent 输出事件，而不是反向调用 agent 内部对象；
- 同一 `sessionId` 的音频事件与文本消息共享会话隔离规则；
- 全双工状态机必须可测试：用户说话、AI 输出、用户打断、AI 停止输出四个状态要用单元测试固定；
- Hume EVI / OpenAI Realtime / ChatGPT Advanced Voice 只作为体验与协议参考，不开源实现不作为工程依赖依据；
- Skylark 的 `DuplexSessionStateMachine / Barge-in / BackchannelFilter` 思路可作为设计蓝本，但其 AgentScope 1.x 集成不照搬。

### 13.3 模型优化与 learning 预留

OpenJarvis 的启发点是「线上强模型帮助优化线下 / 本地模型」，但在 OpenAlice 中不直接照搬其工程实现。OpenAlice 的目标是形成可回滚的数据集 → 评测 → 训练 / 蒸馏 → 路由闭环。

未来模块结构（当前不创建）：

```text
openalice-learning/
└── src/main/java/openalice/learning/
    ├── trace/
    │   └── TraceCollector.java
    ├── dataset/
    │   ├── DatasetBuilder.java
    │   └── DatasetCleaner.java
    ├── teacher/
    │   ├── OnlineTeacherClient.java
    │   └── DistillationSampleBuilder.java
    ├── training/
    │   ├── LocalTrainingClient.java
    │   └── TrainingJobRunner.java
    ├── evaluation/
    │   ├── EvalRunner.java
    │   └── RegressionGate.java
    ├── registry/
    │   ├── ModelRegistry.java
    │   └── ModelVersion.java
    └── routing/
        ├── ModelRouter.java
        └── RollbackPolicy.java
```

闭环：

```text
agent.trace
  ↓ TracePort
learning.trace
  ↓ DatasetBuilder
数据集 / 蒸馏样本
  ↓ OnlineTeacherClient
线上强模型标注 / 生成
  ↓ LocalTrainingClient
本地模型训练 / 蒸馏
  ↓ EvalRunner + RegressionGate
评测与回归门禁
  ↓ ModelRegistry + ModelRouter
模型路由更新
  ↓ RollbackPolicy
失败回滚
```

边界规则：

- `openalice-agent` 不依赖 `openalice-learning`；
- `openalice-learning` 只依赖 `core` 的 trace / evaluation / event 契约，不读取 agent 内部字段；
- 训练任务可以离线执行，不进入在线请求关键路径；
- 模型切换必须通过 `ModelRouter` 显式路由，不允许训练完成后隐式替换；
- 每个模型版本保留评测结果、数据集指纹与回滚入口。

### 13.4 OpenHanako 式能力地图

对 OpenHanako / HanaAgent 的参考重点是能力地图，而不是照搬 Electron + Node.js + Pi SDK 架构。

| 能力 | OpenAlice 第一位置 | 未来拆分 | 说明 |
| :--- | :--- | :--- | :--- |
| 多 Agent / 多人格 | `openalice-agent/runtime` | `openalice-runtime` | 每个 agent 独立 persona、工具权限与运行时配置 |
| 子 Agent / 委派 | `openalice-agent/runtime` | `openalice-runtime` | 主 agent 调度，不共享未授权 memory |
| 人格模板 / 角色卡 | `openalice-agent/persona` | `openalice-persona` | 支持导入导出，但导入必须校验权限与安全 |
| 技能包 | `openalice-agent/tool/skill` | `openalice-skills` | 技能是受控工具 + prompt / 资源组合 |
| 插件系统 | 先由 tool 扩展承载 | `openalice-plugin` | 插件不能绕过 `ToolContext` 与权限 |
| 定时任务 / 心跳 / 主动行为 | `server` 调度入口 + `agent` 执行 | `openalice-worker` | “何时触发”和“做什么”分离 |
| 外部渠道 | `openalice-server/channel` | `openalice-channel` | Telegram / 飞书 / 微信等只做适配 |
| 附件与工作台文件 | `core.domain.message` | `openalice-media` / `openalice-artifacts` | 文件有稳定 ID、权限与生命周期 |
| 本地优先数据 | `openalice-memory` | store 模块 | 不把本地文件当成散乱全局状态 |
| 安全沙盒 / 权限 | `agent.tool` + `server.security` | `openalice-security` | 权限是核心边界，不是 UI 配置项 |

### 13.5 后续拆分阈值

满足以下任一条件时，才把能力从既有模块拆出：

1. 该能力已有真实实现与测试，不只是设计想法；
2. 变更原因与现有模块明显不同；
3. 依赖集合需要独立治理（例如训练依赖、音频 SDK、插件 classloader）；
4. 包内超过约 30 个类或显著影响模块可读性；
5. 需要独立发布 / 独立测试周期；
6. 用户明确确认该能力成为产品主线。

拆分前必须先写 ADR，说明依赖方向、测试策略、回滚方式与对 Phase 1 架构的影响。

---

## 14. 架构守护规则

1. `openalice-core` 不依赖 `memory / agent / server`；
2. `openalice-memory` 不依赖 `agent / server`；
3. `openalice-agent` 不依赖 `server`；
4. `openalice-agent` 不直接依赖 `openalice-memory`；
5. 只有 `openalice-server` 生成 Spring Boot executable jar；
6. Spring Web 依赖只进入 `openalice-server`；
7. AgentScope Harness 依赖集中在 `openalice-agent` 与必要扩展模块；
8. 数据库 / Redis / pgvector 依赖集中在 `openalice-memory` 或未来 store 模块；
9. Agent 不保存会话级可变字段；
10. 记忆读写必须通过 `MemoryPort`；
11. 工具访问记忆必须通过 `ToolContext`；
12. 语音、学习、插件、多 Agent 等新能力先通过 core 契约和既有模块包预留；
13. 不因为未来可能拆分而提前创建空模块；
14. 插件与技能不得绕过权限系统；
15. `openalice-agent` 不依赖 `openalice-learning`，学习闭环只消费 trace / event 契约；
16. `openalice-core` 不引入 ASR / TTS / RTC / 训练 SDK；
17. 参考项目不能直接推翻本 ADR，必须先说明适用性与差异。

---

## 15. 实施顺序

架构确认后，Phase 1 建议按以下顺序初始化：

1. `service/pom.xml` Maven 聚合父工程；
2. `openalice-core`：最小领域模型、`MemoryPort`、`TracePort`、`ToolContext`；语音与学习接口是否随 Phase 1 创建空契约，待用户确认；
3. `openalice-memory`：`InMemoryMemoryPort`，用于跑通链路；
4. `openalice-agent`：最小 AgentScope Harness 装配；
5. `openalice-agent`：双 session 并发隔离冒烟测试；
6. `openalice-server`：最小 HTTP / SSE 接口；
7. `web/README.md`：前端占位说明；
8. 验证依赖方向、编译、测试与启动；
9. 用户 review 后再进入持久化与真实记忆算法。

---

## 16. 被放弃或暂缓的方案

| 方案 | 结论 | 原因 |
| :--- | :--- | :--- |
| 根目录直接放四个后端模块 | 暂缓 | 能力清晰，但根目录噪声较大 |
| 完全复刻现有单模块参考项目 | 不采用 | 边界靠自觉，不利于长期自研记忆与 agent |
| 直接拆微服务 | 不采用 | 当前规模不必要，运行复杂度过高 |
| 一开始拆十几个模块 | 不采用 | 过度设计，维护成本高 |
| `agent` 直接依赖 `memory` 实现 | 不采用 | 造成核心模块耦合，改为端口注入 |
| Phase 1 直接接 PostgreSQL / pgvector | 暂缓 | 先用内存实现验证依赖方向与全链路 |
| `core` 增加 `impl` 包 | 禁止 | core 只放稳定抽象，不放实现 |
| 为了测试让 agent 依赖 memory | 禁止 | agent 测试使用 fake port |

---

## 17. 待确认问题

1. 顶层目录是否确定使用 `service/` 与 `web/`；
2. Java 根包是否采用 `openalice.*`，还是使用 GitHub 域名前缀；
3. `MemoryPort`、`TracePort`、`ToolContext` 是否全部在 Phase 1 落入 core；
4. persona 先放在 `openalice-agent/persona` 是否确认；
5. learning 模块是否命名为 `openalice-learning`；
6. `web/` 技术栈何时确认；
7. `SpeechRecognitionPort`、`TextToSpeechPort`、`VoiceSessionPort` 是否 Phase 1 创建空契约；
8. `ModelEvaluationPort` 是否 Phase 1 创建空契约；
9. 首版语音是点按半双工，还是允许浏览器端轻量连续收音；
10. 语音能力何时从 `server.stream.voice` 拆出 `openalice-speech`；
11. 架构确认后是否立即重绘正式架构图。

---

## 18. 参考资料

- Maven Multiple Modules Project
- Spring Guide: Multi-Module Projects
- Alibaba P3C / 《Java 开发手册（黄山版）》
- Google Java Style Guide
- AgentScope Java 2.0 Quickstart
- AgentScope Java Harness Architecture
- AgentScope Java Going to Production
- AgentScope Java Training Integration
- AgentScope Java 官方仓库
- OpenJarvis
- Hume AI EVI 文档
- OpenAI Realtime API 文档
- openhanako / HanaAgent
- Skylark 全双工语音链路参考
