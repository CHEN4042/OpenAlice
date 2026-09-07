# 08 · 单模块收敛决议（修订 ADR 06）

| 项目 | 内容 |
| :-- | :-- |
| 状态 | ✅ 已确认 |
| 日期 | 2026-09-07 |
| 修订 | **修订 ADR 06** 的「根目录四 Maven 模块」布局 |
| 阶段 | Phase 1 implementation |

## 1. 背景

四模块（core / memory / agent / server）虽边界清晰，但对唯一开发者（边做边学）产生三个负担：

1. 一个功能被拆到四个目录，读代码要跨模块跳转，「每个模块对应哪个功能」难以对上；
2. 编译期强制的边界，换来的是 IDE 里多模块导航与 Maven 聚合的复杂度；
3. 模块边界内仍缺 service 层，编排揉在 `AgentRuntime` 里，`controller` 直接依赖 `runtime + memory` 两个接口。

对照本地 Jarvis 参考工程（单 Spring Boot 模块 + controller/service/dao 顶层分包），用户决定收敛为单模块。

## 2. 决策

**Maven 四模块合并为一个 Spring Boot 应用模块**，根 `pom.xml` 即应用（`io.openalice:openalice`），源码根 `src/main/java`，根包 `openalice`，按职责顶层分包：

```text
src/main/java/openalice/
├── OpenAliceApplication.java   # 启动类（组合根 = Spring 容器）
├── model/        # 纯 POJO / 值对象：ChatMessage · MessageRole · UserId · SessionId
├── port/         # 端口接口：MemoryPort
├── memory/       # MemoryPort 适配器：InMemoryMemoryPort（将来 PostgreSQL 实现）
├── agent/
│   ├── runtime/  # AgentRuntime · AgentScopeAgentRuntime · AgentRuntimeFactory · ChatResult · AgentRuntimeProperties
│   └── llm/      # LlmProvider · LlmModelFactory · DeterministicChatModel · ConfiguredHttpTransport
├── service/      # ChatService：一次 /chat 的业务编排（存消息 → 问 agent → 存回复）
├── controller/   # ChatApiController · HealthController · ApiExceptionHandler
├── dto/          # ChatRequest · ChatReply · MessageView
└── config/       # OpenAliceConfiguration：Spring 组合根
```

## 3. 关键改动

1. **新增 `service.ChatService`**：编排从 `AgentScopeAgentRuntime` 上移（runtime 不再持有 `MemoryPort`，只做「校验 → 调模型 → 返回回复」）；`controller` 只接 HTTP，依赖 `ChatService`。
2. **组合根换实现**：手写 `CompositionConfiguration`（server 模块）→ `config.OpenAliceConfiguration`（Spring `@Bean` 装配 memory + agent），`ChatService` 用 `@Service` 进入容器。
3. **类可见性统一 public**，对齐 Jarvis 与初学者心智。
4. **删除 0 引用占位**：`PersonaPrompt`、`MemoryQuery`、`MemoryRecord`、`TracePort`（未来按记忆架构/人设需求重建，不提前留死代码）。
5. **包命名消歧**：原 `agent.config` / `agent.model` 并入 `agent.runtime` / `agent.llm`，避免与顶层 `config`（Spring 装配）混淆。
6. 死代码删除、包搬迁、单 pom 合并；`mvn clean test` 全绿（13 tests：model 2 / memory 4 / agent 2 / service 3 / controller 2）。

## 4. 保留的四模块优点（以包纪律替代模块边界）

| 原模块优点 | 单模块中的保留方式 |
| :-- | :-- |
| domain 纯净、port 抽象 | `model/`、`port/` 仍是纯 POJO/接口，靠 AGENTS.md 纪律约束 |
| agent 不依赖 memory 实现 | `agent` 只 import `port.MemoryPort`，且现在运行时连 MemoryPort 都不持有 |
| 换存储只动一处 | 改 `config.OpenAliceConfiguration.memoryPort()` 一个方法 |
| 组合根唯一 | Spring 容器即组合根，配置集中在 `config/` |

接受的代价：失去编译器强制模块边界，依赖规则靠 AGENTS.md + 代码评审维护（必要时可加 ArchUnit 依赖规则测试）。

## 5. 测试分层（修订 ADR 06 §开发规范）

```text
model/      纯单元测试
memory/     存储行为测试（并发、隔离、不可变快照）
agent/      默认 mock 模型跑 runtime（无环境变量 key 时回落 DeterministicChatModel）
service/    fake AgentRuntime + InMemoryMemoryPort，测「先存 user → 回复 → 再存 assistant」编排
controller/ SpringBootTest 全链路（MockMvc）
```

## 6. 遗留与后续治理

- P1 存储仍是 `InMemoryMemoryPort`；接 PostgreSQL（M1 会话消息）时在 `memory/` 下新增适配器，不引入新顶层结构。
- 若未来业务域膨胀（如记忆写入管线、语音），先按 `service/` 或 `agent/` 下再分子包演进，包超过 ~10 个顶层目录再评估拆分模块。
- ADR 06 中「Maven 模块依赖方向、组合根位置」相关结论被本 ADR 取代；其 §3 最小实现范围与 §5 预留原则继续有效。
