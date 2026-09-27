# ADR 14 · 人格契约先行，persona/ 资产加载后置

| 项目 | 内容 |
| :-- | :-- |
| 状态 | ✅ 当前有效 |
| 日期 | 2026-09-20 |
| 关系 | 修订 ADR 07 第 4 项中的「P1 首次启动引导生成 `persona/` 文件」；不改变单用户、稳定 `userId`、PG 持久化与真实模型方向 |
| 阶段 | P1.5 / P1 收口 |

## 1. 背景

Forge 会话已将产品方向判定为 `HARDENED`，并锁定爱丽丝的人格核心：以《碧蓝档案》天童爱丽丝的**行为与价值观核心 + 少量游戏化表达**为准，拒绝通用温柔陪伴外壳、完整复刻、固定名台词与 cosplay。

但仓库原有实现仍是通用 system prompt：

```yaml
system-prompt: "You are Alice, a warm and attentive AI companion."
```

ADR 07 同时计划在 P1 首次启动时生成 `persona/` 文件。若现在立即引入文件加载、初始化向导与用户资料输入，会把尚未经过真实对话验证的行为契约提前固化成资产层接口，违反个人版的最小设计税原则。

## 2. 决策

1. **人格行为契约是产品层真相**：完整规则放在 `_bmad-output/specs/spec-openalice/persona-contract.md`，三信号与回复结构放在 `conversation-policy.md`。
2. **P1 首版用结构化 system prompt 落地**：直接替换 `openalice.agent.system-prompt`，表达角色核心、回应结构、异议协议与边界；不新增 `persona/` 文件、初始化向导或动态加载层。
3. **独立 persona 资产后置**：等真实对话验证行为契约后，再评估迁移到 `identity.md` / `persona-config.yml` / `rules.md` 等资产。
4. **UserProfile 与角色资产分离**：用户名、称呼、偏好、自我介绍属于可修改 `UserProfile`；`userId` 由系统生成、永久、隐藏且不可编辑。
5. **训练与数据集继续暂缓**：不因角色保真目标提前引入微调、数据集或专用推理链路。

## 3. 权衡

| 方案 | 验证速度 | 设计税 | 可维护性 |
| :-- | :-- | :-- | :-- |
| 结构化 system prompt（采用） | 最快，可直接对话验证 | 最低 | 契约未稳定前避免空加载层 |
| 立即落 persona/ + 初始化 | 最慢，需要先做交互与文件生命周期 | 高 | 结构完整但可能按错误规则固化 |
| 继续通用温柔 prompt | 无实现成本 | 低 | 与角色契约冲突，无法验收 |

角色契约首先需要验证“是否真的像爱丽丝、是否能在四类情境中稳定表达立场”。资产层应在契约稳定后承载配置，而不是在之前替代契约。

## 4. 影响

- `src/main/resources/application.yml`：system prompt 改为结构化 Alice 角色契约。
- 测试中的通用 prompt 常量改为中性测试值，避免继续传播旧人格假设。
- 《项目需求说明书》升至 v1.5.1，§5.1 / §5.2、README、CONTEXT、文档索引与记忆设计同步。
- ADR 07 的 persona/ 初始化时点被本 ADR 修订；单用户与固定 `userId` 结论不变。
- M1 PostgreSQL 持久化不受影响，仍是下一代码里程碑。

## 5. 后续

- 用真实对话验证 persona-contract 的四类情境、回复结构与异议协议。
- 若验证通过且出现配置维护需求，再设计 `persona/` 资产加载；若验证失败，先修 prompt 契约，不迁移资产。
