# 06 · Phase 1 根目录四模块布局决议

| 项目 | 内容 |
| :-- | :-- |
| 状态 | ✅ 已确认 |
| 日期 | 2026-09-06 |
| 修订 | 修订 ADR 05 v4 中“`service/` 聚合 Java 后端”的顶层目录结论 |
| 阶段 | Phase 1 implementation |

## 1. 决策

用户确认：**取消 `service/` 聚合层**，Java Maven 模块直接放在仓库根目录。

Phase 1 仓库形态：

```text
OpenAlice/
├── openalice-core/       # 领域模型与端口
├── openalice-memory/     # 自研记忆实现
├── openalice-agent/      # AgentScope agent runtime
├── openalice-server/     # HTTP 组合根与 Spring Boot 壳
├── web/                  # 未来前端占位，不进入 Maven Reactor
├── docs/
├── pom.xml               # Maven 聚合父工程
├── README.md
├── AGENTS.md
└── handoff.md
```

## 2. 保留的架构规则

1. 根目录 `pom.xml` 是 Maven 聚合父工程。
2. 四个 Java 模块直接位于仓库根目录。
3. `openalice-server` 是唯一 Spring Boot executable application。
4. `core / memory / agent` 是 library module。
5. 依赖方向：

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

6. `openalice-agent` 不直接依赖 `openalice-memory`，只依赖 `core.MemoryPort`。
7. `openalice-server.composition` 是唯一组合根，负责把 memory 实现注入 agent。
8. `web/` 不进入 Maven Reactor。
9. AgentScope Java 2.0.2 是 Phase 1 预选底座；如发现关键 API 回归，可回退 2.0.0。
10. Spring Boot 3.5.16 只做 Web 壳，不引入 Spring AI / Spring AI Alibaba。
11. Java 目标版本为 17。

## 3. Phase 1 最小实现范围

Phase 1 不接真实 LLM API、不需要 API key，先跑通：

```text
HTTP
  → openalice-server
  → AgentRuntime
  → AgentScope HarnessAgent
  → DeterministicChatModel
  → MemoryPort
  → InMemoryMemoryPort
  → HTTP response
```

基础能力：

- `POST /api/v1/chat`
- `GET /api/v1/users/{userId}/sessions/{sessionId}/messages`
- 同 user 不同 session 的上下文隔离
- user / assistant 消息写入自研 memory port
- Maven 全模块测试与 server executable package

## 4. 本决议与 ADR 05 的关系

ADR 05 v4 的“模块边界、依赖倒置、开发规范、测试策略、语音 / learning / OpenHanako 式能力预留”继续有效。

仅以下顶层结论被本 ADR 修订：

- 不再使用 `service/` 聚合 Java 后端；
- Maven 父 POM 从 `service/pom.xml` 改为仓库根 `pom.xml`；
- 四个 Maven 模块从 `service/openalice-*` 改为根目录 `openalice-*`。

## 5. 风险与后续治理

- 根目录会直接出现四个 Maven 模块，比 `service/` 方案多一些顶层目录；这是用户为了减少层级、贴近个人项目心智而接受的折中。
- 若未来根目录模块超过 7 个，需要重新评估是否引入聚合目录或按业务域拆仓。
- 仍然禁止提前创建空的 `speech / learning / tools / persona` Maven 模块；先用 core port、包位置和 ADR 预留扩展点。
