# Handoff · OpenAlice（A.L.I.C.E. 爱丽丝）

> 本文件是**给下一位接手本项目的 AI / 工程师的会话交接文档**（session handoff），位于仓库根目录 `handoff.md`（固定在根目录，不进 `docs/`）。
> 接手顺序：**先读本文件** → `docs/index.md`（文档总索引，看概括）→ 按需选读《项目需求说明书》（`docs/项目需求说明书 v1.2.md`）等，不全量通读。
> 维护约定：每次会话结束（换人 / 换会话 / 上下文耗尽前）更新本文件；宁可整体重写，也不叠床架屋。

## 元信息 · Metadata

| 项目 | 值 |
| :--- | :--- |
| **Written** | 2026-09-03（v1 · 会话交接机制建立） |
| **Updated** | 2026-09-06（v10 · 项目更名 OpenAlice + 全仓同步 + 命名体系瘦身） |
| **Status** | `direction-locked` + `renamed` —— 框架方向已定：**Java + AgentScope 2.0**（decisions/01）；项目已更名 **OpenAlice**（品牌形态 **A.L.I.C.E.** · 爱丽丝，工程代号 `openalice`）；**v10 已提交并推送（`4fe654e`，用户指示直接 push）；GitHub 仓库改名 OpenAlice 待用户操作**；尚无代码目录，Step A 探针待用户批准 |
| **Branch** | `main` |
| **Last commit** | 已推送至 `4fe654e`（v10 更名提交，用户指示直接 push；GitHub 仓库改名 OpenAlice 后本地 remote 同步为新地址） |
| **Remote** | 当前 `git@github.com:CHEN4042/OpenLexington.git`（private）；**待用户在 GitHub 将仓库改名为 OpenAlice**（远端 301 自动跳转；本地 `git remote set-url git@github.com:CHEN4042/OpenAlice.git` 一并更新） |
| **Previous handoff** | v1（`d68e5e3`）；v2–v10 为连续会话，本文件滚动刷新 |

---

## 1. 本次任务 · Task

任务分阶段推进；最新一轮为 **v10（2026-09-06）· 项目更名 OpenAlice + 命名体系瘦身**，见下：

**v10（2026-09-06）· 项目更名 OpenAlice（已提交 `4fe654e` 并推送，用户指示直接 push）**
- 用户拍板：项目名 **OpenLexington → OpenAlice**（工程代号 `openalice`）；品牌 / 人设形态 **A.L.I.C.E.**（点分大写），中文名 / AI 自称 **爱丽丝**（中英均可）；读音 `/ˈælɪs/`。
- **删除**：全称递归展开为九特质（Listening / Empathetic / Xenial / Intimate / Nurturing / Growing / Trustworthy / Open-hearted / Navigator）、字母↔需求映射表、"名字即人格说明书"、README 特质展开行、战舰少女R / 航母 / lex- 词根致敬——命名不再承担"人格说明书"职能。
- **改写**：九特质人格表述 → 不绑定名字的普通人格条目（倾听 / 共情呵护 / 温柔亲切 / 坦诚边界 / 亲密 / 成长可信赖 / 生活领航 / 情绪状态），语义保留（需求书 §5.1）；"九特质开关"→"人格维度开关"；"Navigator 特质"引用 →"生活领航"表述。
- **灵感来源（更新）**：碧蓝档案 爱丽丝（主）+ 爱丽丝梦游仙境（致敬）+ Red Queen（初代 AI 意象，呼应）。
- **无关联声明**：与 `TraderAlice/OpenAlice` 等第三方同名 / 近似项目无任何关联（README / 需求书 §0.4 / ADR 04 / AGENTS 红线）。
- **落地清单**：需求书升 **v1.3**（§17 只追加不改旧行）；新增 `docs/decisions/04-openalice-renaming.md`；全仓"当前状态"表述同步 openalice / A.L.I.C.E. / 爱丽丝；**git 历史与各文档历史记录（§1 旧 v 段、§3、版本历史旧行、index 整理记录旧行）保留旧名不改写**。
- 待办（用户侧）：GitHub 仓库改名为 OpenAlice；本地 `git remote set-url` 同步。

**v9（2026-09-04）· 方向抉择 + 第一步目标定义（已提交 `8c73b81`，用户已推送）**
- 方向确认：用户明确**保持 Java + AgentScope 2.0 自研**（不转向 TS 生态、不动摇 decisions/01）；OpenHanako / HomeRail 仅作设计 / 效果参考，不抄代码。
- 对标锚点：**旧 Jarvis**（Spring AI 自研工程，本机归档、不入库）= 432 个 Java 文件 / ~45k 行；手写 harness（`agents/` ≈ 9.4k 行：工具 / 技能 / 记忆 / 子 agent / 提示词装配 / SSE 事件）≈ AgentScope 2.0 内置件 → 用 AS 后角色从「实现 harness」转为「**装配 harness + 业务工具 + 人格资产**」。
- **第一步完成定义**：旧 Jarvis `agents/` 层**最小可运行闭环**（能力面约 20%；业务完成度 0、底座完成度 100%）：① 对话 → A.L.I.C.E. 人格回复（persona 文件生效）；② 类型化事件流式（顺带完成 2.0.2 流式回归实测，回写 01）；③ 1 安全工具直接用 + 1 敏感工具触发 ASK；④ 记忆冒烟：重启后可检索上会话关键事实（回答 02 §4 记忆边界重验）；⑤ **workspace 首启自动生成**（AGENTS/SOUL/PROFILE/MEMORY/BOOTSTRAP.md + 目录 + 元数据）。
- 底座范式（参照 02 MelonPaw）：代码侧 = Agent 工厂单例装配 `HarnessAgent.builder()`（toolkit / workspace / middleware / memory / compaction / stateStore / model / permission）；workspace 侧 = 模板 md 随首启灌入。
- 落地两跳：**跳 1** = 仓库外探针（/tmp：Quickstart + streamEvents + @Tool + ASK + workspace 自生成 → 验证 2.0.2，产出装配片段回写 01/02）；**跳 2** = 用户批准后按需求书 §7.2 建 Maven 四模块骨架。**跳 1 已提议，等待用户批准。**

**v8（2026-09-04）· Git 协作规范（已提交）**
- 用户指示（调研 Lumina AGENTS.md 的 git 部分后）：**大改动后由 Codex 本地 commit**（Conventional Commits + 中文，大改动带 body）；**push 一律由用户手动执行**；AGENTS.md 新增「Git 协作规范」小节，handoff 同步旧表述。

**v5–v7（2026-09-04）· 仓库文档整理（已提交 `884c642`，用户手动推送）**
- 根目录保留 README / AGENTS / handoff / .gitignore；正文文档归档 `docs/`；docs/README.md 更名 index.md 升级为**文档总索引**；需求 / 架构文件名用中文描述并**保留版本号**（`项目需求说明书 v1.2.md` / `系统架构图 v2.0.md`，v7 定稿）；handoff 固定在仓库根目录。

**v4（2026-09-04）· 命名收敛（已提交 `8625a89`，已推送）**
- 命名体系四层 → 两层：工程代号 `openlexington`（对齐仓库 OpenLexington）；移除短缩写 L.E.X.I. 与小名 Lexi（莱克茜）；AI 自称统一为**列克星敦 / L.E.X.I.N.G.T.O.N.**（中英均可）——**v10 起再更名为 OpenAlice / A.L.I.C.E.（爱丽丝）**。

**v2–v3（2026-09-03/04）· AgentScope 评估 + 公开化脱敏**
- 完成 **AgentScope Java 2.0 vs Spring AI 实证差距分析**（公开资料 + 既往 Java Agent 工程实测，结论见 decisions/01）；经用户确认建立 docs/ 档案。
- v3：仓库当前 private、未来可能转 public → 全仓按 **public 标准**清洗（组织标识、邮箱、本机路径、SSH 细节等）；含工程细节的 docs/02 移出仓库本地归档，01 解除依赖后保留结论。

---

## 2. 当前状态 · Current State

- `main` 分支：已推送至 `4fe654e`（v10 更名提交，用户指示直接 push）；GitHub 仓库改名 OpenAlice 待用户操作。
- 方向状态：框架方向**已锁定 Java + AgentScope 2.0**（decisions/01）；第一步目标已定义（§1 v9）；**尚无任何代码目录**（Maven 模块 / `web/` / `persona/` 均未创建），探针与骨架均待用户批准。
- 文件（当前）：根目录 `README.md` / `AGENTS.md` / `handoff.md` / `.gitignore`；`docs/`：`index.md`（总索引）、`项目需求说明书 v1.2.md`（正文 v1.3）、`系统架构图 v2.0.md`、`CONTEXT.md`、`decisions/01-agentscope-vs-springai.md`、`decisions/02-melonpaw-reference.md`、`decisions/03-skylark-voice-reference.md`、`decisions/04-openalice-renaming.md`（v10 新增）。
- ✅ **隐私策略（v3 起，无论 private / public 一律适用）**：默认按 **public 标准**维护——不出现组织名称与标识、内部项目 / 仓库 / 部署细节，及个人邮箱、本机绝对路径、SSH 细节；入库前工具扫描验证；转 public 前须先重写 git 历史（历史含旧敏感内容，见 §7）。

---

## 3. 已完成工作 · What Was Done（要点）

- v1（`d68e5e3`）：SSH 认证修复；需求书勘误（未来日期 → 2026-09-03、记忆编号 L1-L3 → M1–M3、VAD 断句 700ms、.gitignore 加 .DS_Store）；撰写 handoff v1。
- v2（评估）/ v3（脱敏）：见 §1。
- v4（`8625a89`）：命名收敛四层 → 两层（openlexington / L.E.X.I.N.G.T.O.N. 列克星敦）。
- v5–v7（`884c642`）：文档整理（docs/ 归档 + index 总索引 + 中文版本号文件名）。
- v8（`36b0d83`）：Git 协作规范（AI 本地 commit、push 留用户）。
- v9（`122bfb5` MelonPaw 02 + `8c73b81` Skylark 03 / index 修订 / handoff v9）：调研收尾。
- v10（未提交）：项目更名 OpenAlice + 全仓同步 + 命名体系瘦身（清单见 §1）。

### 3.1 git 历史备忘（更早提交）

```
b45e6cb  Initial commit
7f49c68  Update README.md
be86733  docs: 修正需求说明书日期/记忆层级编号/VAD表述，并忽略 .DS_Store
d68e5e3  docs: 新增 handoff.md 会话交接文档（v1）
8625a89  docs: 命名收敛 v1.2.1（工程代号 openlexington，双层命名，移除 L.E.X.I./Lexi）
884c642  docs: 系统架构优化（文档整理 v5–v7）
36b0d83  docs: 新增 Git 协作规范（AI 本地 commit，push 留用户）
122bfb5  docs: 新增 02 MelonPaw 参考评估（AS 2.0 工程范式实证）
8c73b81  docs: 调研收尾（03 Skylark 参考评估 + handoff v9 方向定稿）
```

> ⚠️ **git 历史含旧敏感内容**（个人邮箱、docs/02 历史版本等）：仅清洗工作区文件不够——**转 public 前必须重写历史**（squash / filter-repo），需用户明确指示后执行。

### 3.2 走过的弯路 · Dead Ends（下一位勿重蹈）

- 推送纪律演进：早期规则曾为「完成任务先停下汇报、等明确指示再 commit / push」；**2026-09-04（v8）起改为「大改动后 AI 本地 commit、push 一律留用户手动」**（见 §7）；commit 前仍须先跑脱敏扫描。
- 误以为"脱敏=写作时避开"即可：写作之外还要**工具扫描验证**（grep 邮箱/路径/账号/单位关键词），并覆盖 **git 历史与提交文案**；docs/02 先按"只去标识"处理，后按用户要求整份移出仓库。
- 沙箱可写目录曾指向已删除旧路径导致进程启动失败：命令显式 `workdir` 到仓库即可恢复（本仓库相关命令一律显式指定工作目录）。

---

## 4. 关键决策 · Key Decisions

| 决策 | 理由 | 状态 |
| :--- | :--- | :--- |
| 项目名 **OpenAlice**（品牌 / 人设形态 **A.L.I.C.E.** · 爱丽丝，/ˈælɪs/）；工程代号 **openalice**；AI 自称**爱丽丝 / A.L.I.C.E.**（中英均可）；删除九特质递归展开与旧致敬，改 Alice 三重灵感 + 无关联声明 | 用户指示（2026-09-06 更名 OpenAlice；对应 handoff v10 / 需求书 v1.3 / ADR 04） | ✅ 已定稿并提交（v10 · `4fe654e`） |
| 命名沿革：J.A.R.V.I.S. → L.E.X.I.N.G.T.O.N.（列克星敦，v1.2 / `8625a89`）→ **OpenAlice（A.L.I.C.E. 爱丽丝，v1.3 / v10）** | 用户两次更名指示 | ✅ 历史已定稿 |
| 后端 Agent 框架选 **AgentScope Java 2.0**；版本策略 **2.0.2 实测、遇回归回退 2.0.0** | decisions/01 完整评估：AS 白送 harness 层；2.0.1 曾有流式回归 issue | ✅ **已评估定稿**（待 Phase 1 实测回执） |
| ~~Java 21 + Spring Boot 3 + Spring AI Alibaba~~ → **不引 Spring AI / SAA；Spring Boot 仅作 Web 壳（可选）** | AS 无 Spring 依赖、纯 POJO；SAA 仅对齐 Spring AI 1.1.x；官方模型清单无 Qwen | ✅ **已推翻并更新**（decisions/01 §3） |
| 记忆三级自研 **M1 Redis / M2 PG / M3 pgvector**，AS Memory 仅桥接 | "记忆是灵魂，不外包" | ⚠️ 原则已定，实现待 Phase（不受选型影响） |
| 首版范围：单用户、半双工语音 + 文本兜底、Web UI | 范围控制（需求书 §2.2 P0/P1/P2/暂缓） | ✅ 已定稿 |
| 目录结构：正文文档统一归档 `docs/`（index + 需求书 v1.2 + 架构图 v2.0 + CONTEXT + decisions），`handoff.md` 留在仓库根目录；Maven 模块 / `web/` / `persona/` 暂不创建 | 用户分步确认制 + v5–v7 文档整理指示 | ✅ 文档整理已提交（`884c642`，用户推送）；代码目录挂起 |
| 方向：**保持 Java + AgentScope 自研**，不转向 TS 生态；OpenHanako / HomeRail 仅设计与效果参考 | 用户 2026-09-04 明确；与「记忆是灵魂 + 算法 Java 锻炼」初衷一致 | ✅ 已定稿（v9） |
| 第一步里程碑：对标旧 Jarvis `agents/` 层**最小可运行闭环**（对话 + 流式 + 工具 + ASK + 记忆冒烟 + workspace 首启自生成），不做产品完整度 | 旧 Jarvis 手写 harness ≈ 9.4k 行 ≈ AS 2.0 内置件；底座一次成型后只填充 | ✅ 目标已定义（v9），待执行 |
| groupId `io.github.CHEN4042.openalice`（工程代号词根，预选） | GitHub 用户名已确认 CHEN4042 | ✅ 待建工程时用 |

---

## 5. 未决问题 · Open Issues

- 🟡 **AS 评估完成但未实测 → 已转 Step A 探针计划**：2.0.2 的事件流 / 权限 / 沙箱 / Memory 桥接是否符合文档描述，由**仓库外探针（跳 1）**先行验证（尤其流式首 token 延迟与 02 §4 记忆边界）；**探针待用户批准开跑**。
- 🟡 需求说明书 §15 待定事项未调研：LLM 正式选型、ASR/TTS、云服务商、人格语料、VAD 终端归属。
- 🟡 工程命名（Maven 模块 / 包名 / Redis / 环境变量前缀 = `openalice` 词根）为**预选**：代码落地前可能再调（需求书 §0.3 已标注）。
- 🟡 **跳 1 / 跳 2 均待用户批准**：跳 1 = /tmp 探针验证 2.0.2（仓库外，不开代码目录）；跳 2 = 按需求书 §7.2 建 Maven 四模块骨架（届时才创建代码目录，且需用户明确指示）。
- 🟢 系统架构图 v2.0（PlantUML）尚未实际渲染验证（低风险）。
- 🟢 记忆 M1–M3 实现细节（Redis/PG/pgvector 选型与 schema）未设计。
- 🟡 **git 历史含未脱敏内容**（个人邮箱 + docs/02 旧版本等）：转 public 前需重写历史（filter-repo 或重建干净分支），待用户排期指示。
- 🟡 **GitHub 仓库改名 OpenAlice + 本地 remote set-url**（用户侧待办，本仓库 v10 文档已按新名撰写）。

---

## 6. 给下一位 AI 的下一步 · Next Steps

按顺序执行，每步可独立验证：

1. **（等用户批准）跳 1 · 仓库外探针**：在 /tmp 起 AS 2.0 Quickstart 最小工程，依次验证：① 文本对话（裸 `main` + `HarnessAgent.builder()`）→ ② `streamEvents()` 类型化事件流式 → ③ 1 个 `@Tool` → ④ 1 条 ASK 权限规则 → ⑤ workspace 首启自动生成（模板 md 灌入）。产出：2.0.2 流式回归结论 + 装配代码片段 → **回写 decisions/01（版本结论）与 02（§4 记忆边界重验）**。探针在仓库外进行、不进 repo；API 以 2.0.2 实测为准（MelonPaw 停在 RC3，勿照抄旧 API）。
2. **（用户批准后）跳 2 · 进 repo 建骨架**：按需求书 §7.2 建父 POM + `openalice-{domain,application,infrastructure,server}` 四模块，groupId `io.github.CHEN4042.openalice`；装配范式参照 02 MelonPaw（application = Agent 工厂、infrastructure = 工具 / 记忆 / 文件系统实现、server = Spring Boot 壳 + SSE/WS 网关 + InitCommand）；`web/`、`persona/` 另行指示再建。
3. 骨架落成后跑通最小 `HarnessAgent` 对话 + 流式 → 进 Phase 1 验收（WS 首 token p95 < 2s、权限 / 沙箱满足需求书 3.2）。
4. 模型 key 未定：默认 `dashscope:qwen-plus`（读 `DASHSCOPE_API_KEY`）作示例；正式选型仍待定（§15）。
5. **（转 public 前，须用户明确指示）重写 git 历史**：清除个人邮箱与旧敏感内容（filter-repo 或 squash 为干净历史），重写后所有本地 clone 需重新同步。
6. **会话结束前更新本文件**（根目录 `handoff.md`）：刷新 Updated / Last commit / 各章节，附本次 commit hash 供 `git log` 对账。

---

## 7. 红线 · What NOT to Do

- 🚫 **push：一律由用户手动执行**。Codex 只做本地 commit（大改动完成后自行提交，遵循 Conventional Commits + 中文，见 AGENTS.md「Git 协作规范」）；**未经用户指示不执行 `git push`** 及远端操作（改写历史 / 转 public 处理）。
- 🚫 **未经用户确认，不要创建代码目录 / 代码文件**（Maven 模块、`web/`、`persona/` 一律等指令）；`docs/` 内文档整理（重命名 / 归档 / 索引）可按用户指示执行。
- 🚫 **公开化脱敏红线（v3 起，无论 private / public 一律适用）**：
  - 不出现**单位/组织相关字眼与信息**（组织名称与标识、内部项目名、内部仓库地址、内网/统一认证等部署事实，及任何可关联到具体工作单位或工作环境的内容）；
  - 不出现**个人敏感信息**：邮箱、手机/地址、本机绝对路径、设备/用户名、SSH 密钥细节；
  - 提交身份只用 GitHub 账号邮箱（noreply 优先），**工作邮箱不得出现在 git 历史**；
  - 入库前工具扫描验证（grep 邮箱/路径/账号/单位关键词）；转 public 前先重写历史。
- 🚫 **不要把"技术预选"当"已定架构"**：AS 尚未实测，随时可能回退 2.0.0 或双轨。
- 🚫 **不要混用编号**：记忆层级 **M1–M3**；隐私分级 **L1/L2**（需求书 §3.3）。维度不同。
- 🚫 命名体系 / 人格 / 兜底三件套 / 致敬声明 = 项目灵魂：**无用户明确指示不改动**（2026-09-04 收敛双层命名，2026-09-06 更名 OpenAlice 并瘦身；后续再改需用户指示）。
- 🚫 不提交 `.DS_Store`、SSH 私钥、口令、token；**不混淆本仓库与第三方同名 / 近似项目**（如 `TraderAlice/OpenAlice` 等；本仓库归属 **CHEN4042**）。
- 🚫 不要大包大揽一次性实现：当前阶段"文档只做参考、逐步推进"，先对齐再动手；**git 历史与文档历史记录保留旧名不改写**。

---

## 8. 项目背景 · Context for Continuation

- **项目是什么**：面向**唯一用户本人**的 AI 陪伴助手 —— "一个倾听的、共情的、温柔的、永不忘记你的存在"。核心价值是**情绪陪伴 + 深度交互**，记忆是灵魂。
- **命名**：原名 J.A.R.V.I.S. → L.E.X.I.N.G.T.O.N.（列克星敦）→ 现名 **OpenAlice**（品牌 / 人设形态 **A.L.I.C.E.** · 爱丽丝，/ˈælɪs/）；工程代号 `openalice`（对齐目标仓库 OpenAlice）；AI 自称**爱丽丝 / A.L.I.C.E.**（中英均可），不设短缩写与小名；技术文档保持客观，行文尊重设定。
- **灵感来源**：碧蓝档案 爱丽丝（主）+ 爱丽丝梦游仙境（致敬）+ Red Queen（呼应）；交互设计参考 HomeRail（语音优先 + 生成式 UI + DAG 可追溯），OpenJarvis / jarvis-ai-platform 分层思路参考——参考不抄代码。
- **交互形态**：语音为主（半双工点按说话，首版）、文本兜底；前端由 AI 全权开发，用户不写前端。
- **开发节奏**：6 个月 / Phase 1–9（需求书 §12），每 Phase 留 10–20% 缓冲；本地 LLM 为 Ollama + Qwen2.5，正式待选（通义 / DeepSeek / GLM）。
- **环境事实**：macOS + zsh；GitHub 账号 CHEN4042；开发在 2 台 macOS 间进行，文档**不记录本机绝对路径/用户名**（防公开泄露）。
- **沟通偏好**：中文沟通（技术名词可用英文），回答保持中文；提交习惯：Codex 本地 commit（Conventional Commits + 中文）、**push 由用户手动执行**，直接推 `main`，无 PR 流程。

---

## 9. 验证命令 · Verification Commands

```bash
# 仓库状态与历史（在仓库根目录执行）
git status && git log --oneline

# SSH 认证（应输出 Hi CHEN4042! ...）
ssh -T git@github.com

# 远端确认（GitHub 仓库改名 OpenAlice 后应为 git@github.com:CHEN4042/OpenAlice.git）
git remote -v

# 文件清单（根目录 README/AGENTS/handoff/.gitignore；docs/ = index + 需求书 + 架构图 + CONTEXT + decisions/01–04）
ls -1 && ls docs/ && ls docs/decisions/
```

---

## 10. 关键文件 · Key Files

| 文件 | 作用 |
| :--- | :--- |
| `handoff.md` | 本文件（仓库根目录）：会话交接入口，先读它 |
| `docs/项目需求说明书 v1.2.md` | **核心规格**（正文版本 v1.3）：命名体系 / 功能 / 技术选型 / 架构 / 记忆 / 里程碑 / 风险 |
| `docs/系统架构图 v2.0.md` | PlantUML 源码：AgentScope 运行时版架构图 |
| `docs/CONTEXT.md` | 共享语言与术语速查（速查，非规格） |
| `README.md` | 项目门面：Logo、状态徽章、文档导航 |
| `AGENTS.md` | Codex 入口指引（须留在仓库根目录） |
| `docs/index.md` | 文档总索引（含每份文档概括、`decisions/` 决策清单与编号约定） |
| `docs/decisions/01-agentscope-vs-springai.md` | **选型分析（✅ 定稿）**：AgentScope 2.0 胜出 + 版本策略 |
| `docs/decisions/02-melonpaw-reference.md` | MelonPaw 参考评估：AS 2.0 工程范式（工厂装配 / workspace 文件即配置 / 工具·权限）；§4 记忆边界待 Phase 1 重验 |
| `docs/decisions/03-skylark-voice-reference.md` | Skylark 参考评估：语音链路组件地图（VAD / ASR / TTS / RTC），供 §15 语音选型实证 |
| `docs/decisions/04-openalice-renaming.md` | **v10 新增**：OpenLexington → OpenAlice 更名决议（词形映射 / 删除清单 / 灵感来源 / 无关联声明） |
| `.gitignore` | 忽略规则（含 `.DS_Store`） |

> 注：早期 `docs/02`（既往 Spring AI 自研实证，含旧 Jarvis 工程细节）已于 2026-09-04 移出仓库并本地归档，不入库；现 `docs/decisions/02` 为新参考评估（MelonPaw），两者不同。

---

## 11. 参考资料 · References（精简）

**Handoff 实践参考（v1 沿用）**
- [wjgoarxiv/agent-handoff-skill](https://github.com/wjgoarxiv/agent-handoff-skill) —— 7 必备章节 + `[UNVERIFIED]` 标记 + 整体替换策略
- [yoheinakajima/activegraph · HANDOFF.md](https://github.com/yoheinakajima/activegraph/blob/main/HANDOFF.md)（BabyAGI 作者）—— Current State → 纪律 → 交接下一步
- [tw93/Luo · HANDOFF.md](https://github.com/tw93/Luo/blob/master/HANDOFF.md) —— 中文、版本化技术交接范本
- [Anthropic · Claude Code Memory](https://code.claude.com/docs/en/claude-md) —— AGENTS.md/CLAUDE.md 最佳实践

**AgentScope Java 2.0（评估依据）**
- 官方中文文档：https://java.agentscope.io/v2/zh/docs/index.html ；Quickstart：https://java.agentscope.io/v2/zh/docs/quickstart.html ；Release Notes：https://java.agentscope.io/v2/zh/docs/others/release-notes.html
- GitHub（News 能力清单）：https://github.com/agentscope-ai/agentscope-java
- Harness 详解（2026-08 博客）：https://java.agentscope.io/v2/zh/blogs/agentscope-v2-explained.html
- Maven Central 版本核查：https://repo1.maven.org/maven2/io/agentscope/agentscope-harness/maven-metadata.xml

**参考工程（decisions/02、03 评估对象与设计灵感）**
- [melon1010/MelonPaw](https://github.com/melon1010/MelonPaw) —— AS 2.0 工程范式实证（工厂装配 / workspace 文件即配置 / 工具·权限），见 decisions/02
- [Jashinck/Skylark](https://github.com/Jashinck/Skylark) —— 语音链路组件地图（VAD / ASR / TTS / RTC；agent 部分为 1.x 不参考），见 decisions/03
- [monkky/openhanako](https://github.com/monkky/openhanako) —— 桌面陪伴 agent（recency-decay 记忆 / 人格文件体系），设计参考
- [xiaotianfotos/homerail](https://github.com/xiaotianfotos/homerail) —— 语音进、生成式 UI 出（A2UI / Catalog），Phase 2+ 效果目标

**Spring AI 阵营（对照组）**
- 2.0.0 GA：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now ；2.0.1：https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now
- SAA vs AgentScope 定位（官方团队博客）：http://java2ai.com/en/blog/saa-agentscope-announcement/

---
**End of Handoff** — 下一次会话结束时，请按第 6 节刷新本文件（根目录 `handoff.md`）；git 操作须先获用户明确指示。
