---
id: SPEC-openalice
companions:
  - persona-contract.md
  - conversation-policy.md
sources:
  - _bmad-output/forge/openalice-product-idea/forged-idea.md
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability only.

# OpenAlice 产品实施契约

## Why

OpenAlice 面向唯一用户本人：异地工作、独居，日常发生理发、买蛋糕这类生活碎片后，有强烈分享欲，但当前只能发进一个经常没有回应的家庭群。通用陪伴助手能共情，却缺少两件东西：在当下结合真实历史接住具体细节的能力，以及稳定、可辨识、不会沦为空泛回声的爱丽丝人格。现在需要把已经 `HARDENED` 的产品设想收敛成可实施、可验收的契约，并让用户在下一次真实分享时，第一反应可以是先找爱丽丝。

## Capabilities

- **CAP-1**
  - **intent:** 用户可以用文本分享生活碎片，爱丽丝结合当前消息与历史记忆，给出值得继续回答的回应。
  - **success:** 在理发、蛋糕等场景中，回应包含对具体内容的接住、一个可回答的具体问题和一个记忆钩子；只给泛化共情或“我理解了”判定失败。
- **CAP-2**
  - **intent:** 用户可以连续恢复同一段对话，不因服务重启丢失上下文。
  - **success:** 写入多轮历史、重启服务后继续同一 session，下一轮回复仍能引用重启前的内容。
- **CAP-3**
  - **intent:** 用户可以获得具备天童爱丽丝行为核心与稳定立场的回应，包括温和但只说一次的异议。
  - **success:** 按 `persona-contract.md` 的场景验证行为：不是无条件赞美或顺从；异议依据来自勇者/队友价值；表达后把选择权交还用户并停止施压。
- **CAP-4**
  - **intent:** 系统可以区分永久稳定的身份与可修改的用户资料，用户改名不影响历史归属。
  - **success:** `userId` 由系统首次生成且不可编辑；称呼、偏好、自我介绍存入可修改的 `UserProfile`；修改资料后同一用户的历史仍然连续。
- **CAP-5**
  - **intent:** 系统可以按用户当下的沟通意愿决定是否进入长聊，并分别使用话题重要度和跟进容忍度控制长期记忆与主动追问。
  - **success:** 按 `conversation-policy.md` 验证：当下意愿可以独立开启长聊；高重要度不能绕过低意愿；主动追问受容忍度约束；不确定时回到短回应。

## Constraints

- 首版核心只覆盖用户主动分享；爱丽丝主动发起 B 不作为首版成立条件，P1–P3 不主动推送。
- 人格不接受通用“温柔陪伴型”外壳、完整原作复刻、名台词或 cosplay；只保留行为与价值观核心加少量游戏语汇。
- 人格契约首版通过结构化 system prompt 落地；独立 persona 文件、初始化向导、训练与数据集暂缓。
- 单一话题分数不得加权决定是否长聊；长聊只能由当下沟通意愿主开关控制。
- `userId` 首次由系统生成，永久不变、对用户隐藏且不可编辑；可变称呼、偏好与自我介绍归 `UserProfile`。个人版只有一个当前用户来源，不提前建 provider 或依赖注入。
- 个人版优先于企业应用分叉；不为多租户、权限或高并发提前造空接口。
- P1 继续采用 Java 21、Spring Boot、AgentScope Java 2.0.2、单 Maven 模块、HTTP/SSE 文本链路，以及 PostgreSQL 作为唯一外部存储的方向。
- `docs/decisions/` 的 ADR 是技术决策权威；BMAD 产物只补产品层，不覆盖或复制出并列架构体系。

## Non-goals

- 首个实施切片不包含主动发起、语音、Web UI、工具调用、多用户、多人格、模型训练或数据集。
- 首个实施切片不实现话题重要度衰减、跟进容忍度恢复或主动接回触发算法，只保留策略边界。
- 不以完整复刻原作剧情、固定名台词或角色扮演腔调为目标。
- 不为未来企业版提前实现租户、权限或高并发空抽象。
- 不等待成功指标完全操作化后才启动。

## Success signal

- 当用户发生一件事或拿到一样东西后，第一反应从打开微信找父母分享，转为先找爱丽丝分享；这证明她已经接近用户心中可交流的人，而不只是工具。
- 该信号先作为上线后的方向性观察，不阻塞启动；持续时间、重复频率、不同情绪事件上的稳定性与反孤立边界后续再验证。

## Assumptions

- 首版以文本为唯一交互通道。
- 当前代码只作参考，允许后续按本 SPEC 与 ADR 重构，既有实现不是架构真相。

## Open Questions

- 话题重要度是否存在最终遗忘边界，以及跟进容忍度如何恢复，需要后续论文与实验支持；当前不固化成硬规则。
