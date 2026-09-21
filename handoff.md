# handoff.md · OpenAlice 会话交接

> 交接原则：下一位协作者先读本文件，再读 `_bmad-output/specs/spec-openalice/SPEC.md`。
> 当前处于架构重置期：legacy 文档与代码都不能作为新架构基线。

## 0. 当前快照

| 项目 | 内容 |
| :--- | :--- |
| 日期 | 2026-09-21 |
| 项目 | OpenAlice（A.L.I.C.E. 爱丽丝） |
| 阶段 | **pre-rebuild architecture**：需求澄清已闭环，产品契约已按 Boss 裁决修订，准备重新评审架构 |
| 分支 | `main` |
| 产品权威 | `_bmad-output/specs/spec-openalice/SPEC.md` + companions |
| 技术权威 | **尚未生成**；下一步产出 `_bmad-output/architecture/openalice/ARCHITECTURE-SPINE.md` |
| legacy 代码 | `src/` 仅作行为参考；不继承包结构、接口、模型或兼容义务 |
| legacy 文档 | 已移至 `docs/archive/pre-rebuild-2026-09-20/`，默认不读 |
| BMAD runtime | `_bmad/` 已在当前工作区初始化 |
| Git 状态 | 当前工作区有产品契约修订与文档权威重置改动；尚未 commit / push |

## 1. 已完成

- `bmad-forge-idea` 已完成，产品 idea 结论为 `HARDENED`。
- 跨职能需求澄清已完成（真实子智能体 John 提问、Winston 并行评审架构影响，非主 agent 扮演）。Boss 已就长期身份、体验优先级、搜索、记忆、人格、身份模型等问题逐条裁决。
- John 的最终完整性审计结论：**需求澄清足够进入下一阶段，没有阻塞性缺失问题**；未决项均为可后置的算法与体验设计议题。
- 同一轮审计发现 8 处措辞级一致性问题（CAP-2/CAP-5 验收重叠、CAP-1 与最小召回冲突、UserProfile 更新入口缺失、CAP-6 联动强度模糊、未定义的 P1–P3 编号、两类“冲突”混用、独立模块的物理/逻辑歧义、sources 被误当契约）；**8 项已全部修正**，未改变产品意图。
- 本轮新增契约：CAP-6 自动真实搜索问答；CAP-2 收窄为唯一永久主对话；CAP-5 收窄为首版只要求可靠持久化 + 最小召回；身份优先级 A > B > C；爱丽丝是补位而非替代；混合消息先完成任务；歧义分级；搜索来源折叠与冲突披露规则。
- `bmad-spec` 已把产品结论收敛为 CAP-1–CAP-6，并配三个 companions：天童爱丽丝人格卡、回复契约、记忆契约。
- CAP-3 已从“固定 Alice”升级为“人格卡槽位”；首版仍只交付一张内置天童爱丽丝卡，以普通 prompt 加载。
- CAP-5 已从三信号长聊策略改为“独立记忆边界：可靠持久化 + 最小召回”；三信号分离、长聊控制与具体记忆算法全部后置。
- CAP-6 新增：自动判断是否需要真实联网搜索，并基于真实结果回答，不允许编造搜索结论或来源。
- 用户已决定进入彻底重构；当前 M1 实现不再作为后续里程碑，也不要求兼容。
- 旧需求书、记忆设计、代码导览、CONTEXT 与 ADR 01–14 已退出默认阅读路径，统一归档。

## 2. 当前判断

之前的问题是：产品层刚刚澄清，工程层却已经积累了需求书、ADR、README、代码导览和具体实现。它们形成了一套看似权威、实际彼此漂移的旧架构，会持续误导后续 agent。

现在采用新的分工：

- 产品真相只在 SPEC 与 companions；
- 技术真相只在新的 architecture spine；
- 旧资料只承担历史追溯；
- 现有代码只承担行为样例和实现素材；
- 新代码只在 architecture spine 定稿后开始。

## 3. 下一步：`bmad-architecture`

默认走 **Coaching path**。目标不是复述旧目录，而是产出一份短、可执行、约束一致性的 architecture spine，至少明确：

- 系统范式与最小运行边界；
- 对话回合、记忆、身份、模型与持久化之间的所有权；
- 状态在哪里产生、在哪里保存、谁可以修改；
- 模块/包边界和依赖方向；
- 首个可验收垂直切片；
- 明确 deferred 的技术问题。

只有当 spine 定稿后，才进入 story breakdown 和 `bmad-build`。在 spine 完成前，不扩展 `src/`。

## 4. 重构边界

- 不继承 legacy ADR；被重新采纳的结论必须在 architecture spine 中重新确认。
- 不把现有 `ChatService / AgentExecutor / ConversationStore` 等名称视为既定接口。
- 不保留旧包结构；允许换文件、换模型、删测试、重写配置。
- CAP-1、CAP-4 保持；CAP-2 收窄为唯一永久主对话且重启可续；CAP-3 改为首版内置单张人格卡；CAP-5 收窄为可靠持久化 + 最小召回；新增 CAP-6 自动真实搜索问答。
- 架构评审必须按修订后的 SPEC 重新检查上一版 Winston 候选；旧的三信号与记忆算法结论不能直接进入 spine。
- 自然语言纠正/忘记记忆、偏好自动更新与撤回不属于首版；仅保留为 `memory-contract.md` 中的后续研究方向。
- 图片输入输出不进入首版验收，随模型多模态能力后置评估。
- 技术栈当前仍受 SPEC 约束：Java 21、Spring Boot、AgentScope Java 2.0.2、单 Maven 模块、HTTP/SSE、PostgreSQL。若 architecture 阶段认为需要改变，必须先回写 SPEC，而不是静默改口。

## 5. duoagent 工作流

1. **Forge**：压力测试 idea，直至可行动或放弃。（已完成）
2. **SPEC**：把已定产品收敛为机器契约。（已完成）
3. **Architecture**：只记录维持多个独立实现单元一致性的 invariants。（下一步）
4. **Story / Build**：独立 builder 按 SPEC + spine 实施，避免继承当前对话偏见。
5. **Review**：独立 reviewer 检查 diff 与契约，不采信 builder 自述。
6. **Handoff**：更新当前状态、验证证据、风险和唯一下一步。

换机恢复、BMAD 安装与命令记录见 `docs/bmad-workflow.md`。

---
**End of Handoff** — 下一步重新启动 `bmad-architecture` 评审；先按修订后的 SPEC 校正上一版候选，再生成 spine。
