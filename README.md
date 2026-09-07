<div align="center">

# A.L.I.C.E.

### 爱丽丝

*(/ˈælɪs/ · Alice)*

*一个倾听的、共情的、温柔的、永不忘记你的存在。*
*A lifelong AI companion who never forgets.*

![Status](https://img.shields.io/badge/status-phase%201-blue)
![JDK](https://img.shields.io/badge/JDK-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot)
![AgentScope Java](https://img.shields.io/badge/AgentScope%20Java-2.0.2-9cf)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

---

## 项目简介

面向**唯一用户本人**的 AI 陪伴助手 —— 情绪陪伴 + 深度交互，**记忆是灵魂**。

当前 Phase 1 已启动：目标是先跑通 `HTTP → AgentScope → MemoryPort → InMemoryMemory` 的最小可运行链路，再逐步替换真实模型与持久化记忆。

## 架构

```text
OpenAlice/
├── openalice-core/       # 领域模型与端口：不依赖 Spring / AgentScope
├── openalice-memory/     # 自研记忆实现：Phase 1 使用 InMemoryMemoryPort
├── openalice-agent/      # AgentScope HarnessAgent runtime
├── openalice-server/     # Spring Boot HTTP 壳与唯一组合根
├── web/                  # 未来前端占位，不进入 Maven Reactor
└── pom.xml               # Maven 聚合父工程
```

依赖规则：

```text
core ← memory
core ← agent
agent + memory ← server
```

`agent` 不直接依赖 `memory`，只依赖 `core.MemoryPort`；由 `openalice-server` 完成装配。

## 构建与运行

要求：Java 21+、Maven 3.9+。

```bash
# 全量测试
mvn clean test

# 打包 executable server
mvn -pl openalice-server -am package

# 启动
mvn -pl openalice-server spring-boot:run
```

测试接口：

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user","sessionId":"default","message":"你好"}'
```

预期响应：

```json
{
  "userId": "user",
  "sessionId": "default",
  "reply": "收到：你好"
}
```

## 当前边界

- Phase 1 使用 `DeterministicChatModel`，不调用外部 LLM API；
- Phase 1 使用内存存储，进程重启后数据不保留；
- 语音、learning、插件、多 Agent 只保留架构位置，暂不实现；
- `web/` 只是占位，不进入 Maven Reactor。

## 文档导航

| 文档 | 说明 |
| :-- | :-- |
| [docs/index.md](docs/index.md) | 文档总索引：先读它，按需选读 |
| [handoff.md](handoff.md) | 会话交接：下一位 AI / 协作者先读 |
| [docs/项目需求说明书 v1.2.md](docs/项目需求说明书%20v1.2.md) | 项目需求说明书 v1.3（核心规格） |
| [docs/decisions/06-phase1-root-module-layout.md](docs/decisions/06-phase1-root-module-layout.md) | 当前架构决议：根目录四模块布局 |
| [docs/decisions/05-architecture-naming-evolution.md](docs/decisions/05-architecture-naming-evolution.md) | 架构蓝图 v4（顶层布局已被 ADR 06 修订） |
| [docs/CONTEXT.md](docs/CONTEXT.md) | 共享语言与术语速查 |
| [docs/decisions/](docs/decisions/) | 技术决策记录（ADR） |
