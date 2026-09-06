# CONTEXT.md · 共享语言与术语速查

> 目的：给 Codex / 协作者一个最小共识词汇表，避免每次用长句重述概念。
> 性质：速查索引，不是规格。以《项目需求说明书》与 `docs/decisions/` 最新决策为准。

## 一句话定位

A.L.I.C.E.（爱丽丝）= 面向**唯一用户本人**的 AI 陪伴助手 —— “一个倾听的、共情的、温柔的、永不忘记你的存在”。情绪陪伴 + 深度交互，**记忆是灵魂**。

## 命名双层体系

| 层级 | 名称 | 使用场景 |
| :-- | :-- | :-- |
| ① 工程代号 | **openalice** | GitHub 仓库名（OpenAlice）、Maven artifact、Java 根包 |
| ② 全称 | **A.L.I.C.E.**（爱丽丝 · Alice，/ˈælɪs/） | Logo、README 标题、AI 自称 |

> A.L.I.C.E. 非首字母缩写；点分形态仅为仪式 / Logo 呈现。人格表述与名字解耦。

## 当前技术结论

- Agent 框架：AgentScope Java 2.0.2，Phase 1 实测；如关键回归可回退 2.0.0。
- Web 壳：Spring Boot 3.5.16，仅用于 HTTP / 装配，不引入 Spring AI。
- Java：17。
- 构建：Maven 多模块，根目录聚合 `core / memory / agent / server`。
- Phase 1 模型：`DeterministicChatModel`，不依赖外部 LLM API。
- Phase 1 记忆：`InMemoryMemoryPort`，后续替换为 Redis / PostgreSQL / pgvector。
- Agent 不直接依赖 memory 实现，只依赖 `core.MemoryPort`。
- 语音、learning、插件、多 Agent 仅保留架构位置，Phase 1 不实现。

## 高频术语

| 术语 | 含义 |
| :-- | :-- |
| M1–M3（记忆） | M1 短期 → M2 结构化 → M3 语义；必须自研掌控 |
| L1/L2（隐私） | L1 可上云；L2 仅本地 |
| P0/P1/P2/暂缓 | 功能优先级 |
| 组合根 | `openalice-server.composition`，唯一负责依赖装配 |
| 端口 | `openalice-core.port` 中的接口抽象 |
| 全双工语音 | 目标形态：边听边说、可打断；Phase 1 不实现 |

## 行文与编号纪律

- 记忆 = M1–M3；隐私 = L1/L2；优先级 = P0/P1/P2/暂缓，禁止混用。
- 参考项目与需求书只作参考，不约束最终实现。
- 仓库按 public 安全标准维护。
