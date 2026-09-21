# AGENTS.md · OpenAlice（A.L.I.C.E. 爱丽丝）

> 当前阶段：**pre-rebuild architecture**。产品契约已冻结，现有代码与旧工程文档均不再是架构真相。
> 开工先读 `handoff.md`，再读产品契约；架构 spine 生成后，以它作为唯一技术权威。

## 当前权威顺序

1. `handoff.md`：当前状态、下一步与工作区约束。
2. `_bmad-output/specs/spec-openalice/SPEC.md` 及其 `companions:`：产品行为契约。
3. `_bmad-output/architecture/openalice/ARCHITECTURE-SPINE.md`：architecture 阶段完成后的技术权威。
4. `.memlog.md`：只用于追溯决策过程，不是实施入口。

`docs/archive/pre-rebuild-2026-09-20/` 仅保留历史。除非用户明确要求追溯旧决策，否则不得把它作为产品、架构、包结构或实现依据。

## 当前状态

- Forge 已完成，结论为 `HARDENED`；产品愿景不再重开。
- 跨职能需求澄清（真实 John + 真实 Winston）已完成，John 的完整性审计结论为“足够进入下一阶段、无阻塞缺口”。
- SPEC 已完成；下一步是 `bmad-architecture`，不是直接修改或扩展现有实现。
- 用户已决定彻底重构。当前 `src/` 是 legacy reference，允许作为行为样例看，但不继承其包结构、接口、模型或“兼容义务”。
- 在 architecture spine 定稿之前，不继续扩展现有代码，不根据归档 ADR 恢复旧结构。
- BMAD 项目运行时已初始化在 `_bmad/`；换机恢复与流程见 `docs/bmad-workflow.md`。

## 不可丢失的产品红线

- 身份优先级 **A 私人伙伴 > B Java/算法作品 > C 开源/可分享产品**；B/C 不得反噬 A。
- 爱丽丝是现实关系的**补位**，不是替代；成为唯一倾诉对象是风险信号。
- 面向唯一用户本人；首版核心是用户主动分享生活碎片，爱丽丝结合当前消息与历史记忆接住。首版只有**一条永久主对话**，不按话题拆会话。
- 回复默认包含：具体回应 + 一个可回答的具体问题 + 记忆钩子；泛化共情不算接住。
- 人格方向是可替换、按需加载的“AI 人格卡片”；首版只内置一张天童爱丽丝卡，以普通 prompt 加载，卡片管理与多人格切换后置。人格参考《碧蓝档案》天童爱丽丝的**行为与价值观核心 + 少量游戏化表达**；拒绝通用温柔外壳、无条件赞美、名台词复刻与 cosplay。
- 人格核心与爱丽丝自身偏好稳定；“越来越懂用户”是记忆带来的变化，不是人格改变。
- 首版允许且只允许一条真实搜索链路：自动判断是否搜索，失败先承认不确定，来源默认折叠、追问时才展示，搜索与记忆冲突时先披露再由用户裁决；通用工具平台仍禁用。
- 混合消息先完成任务再简短回应分享；歧义分级：闲聊可先猜，事实/决定/敏感内容必须先问。
- 三信号分离不是首版契约，长聊控制后置。首版记忆只要求**可靠持久化 + 最小召回**（重启后近期细节引用或按话题跨会话召回，任一成立）；召回不足时不得伪造记忆。具体记忆算法待论文与实验，不进入首版产品承诺。
- 爱丽丝可以温和异议，但依据必须来自勇者/队友价值；异议只说一次，随后交还决定权并停止施压。
- 图片、偏好自动更新/纠错/撤回不进首版验收。
- 个人版优先；不为多租户、权限或高并发提前造空接口。

## 工作流（duoagent）

- **Planner / Product agent**：BMAD 负责 Forge → SPEC → Architecture → Story breakdown。
- **Builder agent**：architecture spine 定稿后，由独立实施 agent 只按 SPEC、spine 与 story context 写代码。
- **Reviewer**：独立 review，不以 builder 的自述作为验证。
- **Handoff**：每轮结束更新 `handoff.md`；稳定决策进入 SPEC 或 architecture spine，不把临时对话当契约。

## 开发纪律

- Java 21；当前栈方向以 SPEC 为准，具体结构与边界由 architecture spine 决定。
- 不要提前创建空模块、空接口、provider、多租户或高并发抽象。
- 测试优先覆盖真实行为契约，而不是锁定 legacy 实现细节。
- 公共 API、核心边界或产品契约变化时，同步更新权威文档与 `handoff.md`。
- 仓库按 public 安全标准维护；禁止提交组织信息、个人邮箱、本机绝对路径、token、密钥、日志和构建产物。
- `application-local.yml` 含真实 key，已 gitignore；**绝不提交、cat、打印或导出内容**。

## Git

- commit 只在用户明确指示或 review 确认后执行。
- push 一律由用户手动执行，Codex 不执行 `git push`。
- Conventional Commits：`<type>(<scope>): <中文 subject>`。
- 分支前缀默认 `codex/`，除非用户另有要求。
