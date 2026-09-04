# Handoff · OpenLexington（L.E.X.I.N.G.T.O.N. 列克星敦）

> 本文件是**给下一位接手本项目的 AI / 工程师的会话交接文档**（session handoff）。
> 接手顺序：**先读本文件** → 再读《项目需求说明书 v1.2》（现为 v1.2.1 修订内容）→ 需要架构细节时读《系统架构图 v2.0》与 `docs/`。
> 维护约定：每次会话结束（换人 / 换会话 / 上下文耗尽前）更新本文件；宁可整体重写，也不叠床架屋。

## 元信息 · Metadata

| 项目 | 值 |
| :--- | :--- |
| **Written** | 2026-09-03（v2 · AgentScope 评估完成 + docs 档案建立） |
| **Updated** | 2026-09-04（v4 · 命名收敛：双层命名 + 工程代号 openlexington，移除 L.E.X.I. / Lexi） |
| **Status** | `assessment-done` —— 文档阶段 + AgentScope 框架评估**已完成**（结论：选 AgentScope，见 docs/01）；**Phase 1 实测未启动** |
| **Branch** | `main` |
| **Last commit** | `f08ace5` docs: 公开化脱敏 v3（移除组织/个人敏感信息，docs/02 移出仓库）；**工作区含 v4 命名收敛未提交改动**（含 AGENTS.md / CONTEXT.md 新增） |
| **Remote** | `git@github.com:CHEN4042/OpenLexington.git`（当前 **private**；未来可能转 public → 一律按 public 标准维护） |
| **Previous handoff** | v1（`d68e5e3`） |

---

## 1. 本次任务 · Task

任务分阶段推进；最近一轮（v4）改动已完成、**待用户 review 后提交**：

**v4（2026-09-04）· 命名收敛（本次，未提交）**
5. 命名体系四层 → **两层**：工程代号定为 `openlexington`（对齐 GitHub 仓库名 OpenLexington）；移除短缩写 L.E.X.I. 与小名 Lexi（莱克茜）；
6. AI 自称统一为**列克星敦 / L.E.X.I.N.G.T.O.N.**（中英均可），不再引导用户叫 Lexi；工程命名规范标注"**预选，随架构落地调整**"（用户对实现路径仍犹豫）。

**v2（2026-09-03）· AgentScope 框架评估**
1. 完成 **AgentScope Java 2.0 vs Spring AI 实证差距分析**（公开资料 + 既往 Java Agent 工程实测）；
2. 经用户确认建立 `docs/` 档案（README 索引 + 01 选型分析）。

**v3（2026-09-04）· 公开化脱敏**
3. 仓库当前 private、未来可能转 public → 全仓按 **public 标准**清洗：移除组织/单位相关标识与内部信息（组织名称与标识、内部项目/仓库/部署细节）及个人敏感信息（邮箱、本机路径、SSH 细节等）；
4. 既往工程实证文档 `docs/02` 移出仓库（本地归档），`docs/01` 解除对 02 的依赖后保留结论。

---

## 2. 当前状态 · Current State

- `main` 分支；v3 脱敏 + **v4 命名收敛**改动均在工作区**未提交**（AGENTS.md / CONTEXT.md 为 untracked 新增），待用户确认后提交推送。
- 文件：README / 需求说明书 v1.2（标题 v1.2.1，文件名未改）/ 系统架构图 v2.0 / handoff / AGENTS.md / CONTEXT.md / .gitignore / `docs/`（README + 01）。
- **尚无任何代码目录**（Maven 模块 / `web/` / `persona/` 均未创建）——用户分步确认制，等指令。
- ✅ **隐私策略（v3 起，无论 private / public 一律适用）**：默认按 **public 标准**维护——不出现组织名称与标识、内部项目/仓库/部署细节，及个人邮箱、本机绝对路径、SSH 细节；入库前工具扫描验证；转 public 前须先重写 git 历史（历史含旧敏感内容，见 §7）。

---

## 3. 已完成工作 · What Was Done

### 3.1 上一会话（v1，commit `d68e5e3` 记录）
- 该机 SSH 认证修复；需求说明书勘误（未来日期 → 2026-09-03、记忆编号 L1-L3 → **M1–M3**、VAD 断句口径 700ms、.gitignore 加 .DS_Store）；撰写 handoff v1。细节见 `git show d68e5e3` / `be86733`。

### 3.2 会话 v2（评估）与 v3（脱敏）
- **SSH**：曾无私钥 → 新生成密钥并注册到 GitHub CHEN4042；`ssh -T git@github.com` 验证通过。密钥不入库、换机需重配（细节不写入文档）。
- **实证素材（本机，不入库）**：评估参考了既往 Java Agent 工程（Spring AI 自研）的实测结论——agent 运行时自研成本约万行级；工程归属与路径不写入文档。
- **框架评估（docs/01，结论：选 AgentScope Java 2.0）**：
  - 版本事实（实测 Maven Central）：AS **2.0.2**（2026-08-09）；Spring AI **2.0.1**（2026-08-21，基于 Boot 4.1 / Spring Fw7 / Jackson 3，Boot 3 用不了；官方内核模型清单无 DashScope/Qwen）。
  - 血缘澄清：AS Java **无 Spring 依赖**（Quickstart = 裸 `main` + `HarnessAgent.builder()`）；SAA 只对齐 Spring AI 1.1.x，且 SAA 官方 FAQ 称**未来底层将采用 AS-Java** → 需求书"经 SAA 接入 AgentScope"表述**已过时**。
  - 能力对照：AS 白送 workspace/人格文件、31 种类型化事件、AgentStateStore（内存/Json/MySQL/Redis/PG）、记忆压缩策略族、权限三态 HITL、沙箱（本地/Docker/K8s/云）、子 agent、Channel（钉钉/飞书/企微）、async+scheduled wakeup、多租户隔离；Spring AI 仅在 RAG/向量生态与社区规模占优，且**工具循环/MCP 之外无 agent 运行时**。
  - 版本策略变更：旧预选"锁 2.0.0 规避 2.0.1 流式回归"→ **按 2.0.2 实测，遇流式回归回退 2.0.0**。
- **实证（v3 起不入库）**：既往 Spring AI 自研 agent 的实测表明其**缺 agent 运行时**，选它需自研 Harness（万行级）；支撑实证的 `docs/02` 因含工程细节已于 v3 移出仓库（本地归档）。建议：既有存量工程**不推翻重写**，把 AS 当下一代底座新场景试点。
- **docs/ 档案建立**：commit `a1402b3`（README 索引 + 01 + 02）；v3 将 02 移出仓库，01 解除依赖并脱敏。

### 3.3 Git 历史（本机，全部已推送）
```
40693cb  docs: 刷新 handoff v2（AgentScope 评估收尾；仓库转 private，恢复详细记录）
a1402b3  docs: 建立 docs 档案（AgentScope 选型 01 + Jarvis 实证 02，已按 public 仓库脱敏）
d68e5e3  docs: 新增 handoff.md 会话交接文档（v1）
be86733  docs: 修正需求说明书日期/记忆层级编号/VAD表述，并忽略 .DS_Store
7f49c68  Update README.md
b45e6cb  Initial commit
```

> ⚠️ **git 历史含旧敏感内容**（个人邮箱、docs/02 历史版本等）：仅清洗工作区文件不够——**转 public 前必须重写历史**（squash / filter-repo），需用户明确指示后执行。

### 3.4 走过的弯路 · Dead Ends（下一位勿重蹈）
- 未确认就 commit：用户要求**完成任务先停下汇报、等明确指示再提交**（见 §7 红线第一条）。
- 误以为“脱敏=写作时避开”即可：写作之外还要**工具扫描验证**（grep 邮箱/路径/账号/单位关键词），并覆盖 **git 历史与提交文案**；脱敏范围常漏：个人邮箱、本机绝对路径、密钥相关细节、内部仓库地址。docs/02 先按“只去标识”处理，v3 用户明确要求**不出现任何单位/组织相关字眼与信息** → 整份移出仓库。
- 沙箱可写目录指向已删除旧路径：所有命令显式 `workdir` 到 OpenLexington + `login:false` + `shell=/bin/sh` 可恢复偶发 `CreateProcess` 报错。

---

## 4. 关键决策 · Key Decisions

| 决策 | 理由 | 状态 |
| :--- | :--- | :--- |
| 项目名 J.A.R.V.I.S. → **L.E.X.I.N.G.T.O.N.**（列克星敦）；工程代号 **openlexington**（对齐仓库 OpenLexington）；AI 自称**列克星敦 / L.E.X.I.N.G.T.O.N.**（中英均可），不设小名 | 用户指示（2026-09-04 命名收敛：四层 → 两层，需求书 v1.2.1） | ✅ 已定稿（文档，待提交） |
| 后端 Agent 框架选 **AgentScope Java 2.0**；版本策略 **2.0.2 实测、不过回退 2.0.0** | docs/01 完整评估：AS 白送 harness 层；2.0.1 曾有流式回归 issue | ✅ **已评估定稿**（待 Phase 1 实测回执） |
| ~~Java 21 + Spring Boot 3 + Spring AI Alibaba~~ → **不引 Spring AI / SAA；Spring Boot 仅作 Web 壳（可选）** | AS 无 Spring 依赖、纯 POJO；SAA 仅对齐 Spring AI 1.1.x；官方模型清单无 Qwen | ✅ **已推翻并更新**（docs/01 §3） |
| 记忆三级自研 **M1 Redis / M2 PG / M3 pgvector**，AS Memory 仅桥接 | "记忆是灵魂，不外包" | ⚠️ 原则已定，实现待 Phase（不受选型影响） |
| 首版范围：单用户、半双工语音 + 文本兜底、Web UI | 范围控制（§2.2 P0/P1/P2/暂缓） | ✅ 已定稿 |
| 目录结构：`docs/` **已建**（仅公开安全内容，02 已移出）；Maven 模块 / `web/` / `persona/` 暂不创建 | 用户分步确认制 | 🕐 docs 已建，其余挂起 |
| groupId `io.github.CHEN4042.openlexington`（工程代号词根，预选） | GitHub 用户名已确认 CHEN4042 | ✅ 待建工程时用 |

---

## 5. 未决问题 · Open Issues

- 🟡 **AS 评估完成但未实测**：2.0.2 的事件流/权限/沙箱/Memory 桥接是否符合文档描述，待 Phase 1 验证（尤其流式首 token 延迟）。
- 🟡 需求说明书 §15 待定事项未调研：LLM 正式选型、ASR/TTS、云服务商、人格语料、VAD 终端归属。
- 🟡 工程命名（Maven 模块 / 包名 / Redis / 环境变量前缀 = `openlexington` 词根）为**预选**：用户对实现仍犹豫，代码落地前可能再调（需求书 §0.3 已标注）。
- 🟢 系统架构图 v2.0（PlantUML）尚未实际渲染验证（低风险）。
- 🟢 记忆 M1–M3 实现细节（Redis/PG/pgvector 选型与 schema）未设计。
- 🟡 **git 历史含未脱敏内容**（个人邮箱 + docs/02 旧版本等）：转 public 前需重写历史（filter-repo 或重建干净分支），待用户排期指示。

---

## 6. 给下一位 AI 的下一步 · Next Steps

按顺序执行，每步可独立验证：

1. **先 review 当前未提交改动（v4 命名收敛 + AGENTS.md / CONTEXT.md 新增）**：与用户对齐命名口径（`openlexington` 词根是否沿用、§0.3 预选表是否再调）；**用户明确指示后再 git commit / push**。
2. 随后**通读《L.E.X.I.N.G.T.O.N. 项目需求说明书 v1.2.md》全文**（标题 v1.2.1）：§0 命名体系（双层，动手前必读）、§2.2 P0 范围、§7.2 Maven 模块结构（预选）、§8 记忆 M1–M3、§12 里程碑、§16 风险。
3. **框架评估已完成** → 直接引用 `docs/01` 结论与版本策略，不必重复调研。
4. **与用户确认后**：装 Maven → 按 AS 官方 Quickstart（`java.agentscope.io/v2/zh`）建 Maven 多模块骨架（`openlexington-{domain,application,infrastructure,server}` + `web/` + `persona/`），groupId `io.github.CHEN4042.openlexington`。
5. 骨架落成后跑通最小 `HarnessAgent` 对话 + `streamEvents()` 流式 → 按 §12 Phase 1 验收（WS 首 token p95 < 2s、权限/沙箱）。
6. 模型 key 未定：以 `dashscope:qwen-plus` 为默认示例（读 `DASHSCOPE_API_KEY`）。
7. **（转 public 前，须用户明确指示）重写 git 历史**：清除个人邮箱与旧敏感内容（filter-repo 或 squash 为干净历史），重写后所有本地 clone 需重新同步。
8. **会话结束前更新本文件**：刷新 Written / Last commit / 各章节，附本次 commit hash 供 `git log` 对账。

---

## 7. 红线 · What NOT to Do

- 🚫 **未经用户明确指示，不要 git commit / push**：完成任务先停下汇报，等用户 review 后说"提交/推送"再执行。
- 🚫 **未经用户确认，不要创建任何新目录 / 新文件**（Maven 模块、`web/`、`persona/` 一律等指令）。
- 🚫 **公开化脱敏红线（v3 起，无论 private / public 一律适用）**：
  - 不出现**单位/组织相关字眼与信息**（组织名称与标识、内部项目名、内部仓库地址、内网/统一认证等部署事实，及任何可关联到具体工作单位或工作环境的内容）；
  - 不出现**个人敏感信息**：邮箱、手机/地址、本机绝对路径、设备/用户名、SSH 密钥细节；
  - 提交身份只用 GitHub 账号邮箱（noreply 优先），**工作邮箱不得出现在 git 历史**；
  - 入库前工具扫描验证（grep 邮箱/路径/账号/单位关键词）；转 public 前先重写历史。
- 🚫 **不要把"技术预选"当"已定架构"**：AS 尚未实测，随时可能回退 2.0.0 或双轨。
- 🚫 **不要混用编号**：记忆层级 **M1–M3**；隐私分级 **L1/L2**（§3.3）。维度不同。
- 🚫 命名体系 / 人格 / 兜底三件套 / 致敬声明 = 项目灵魂：**无用户明确指示不改动**（2026-09-04 已按用户指示收敛为双层命名，需求书 §0 已同步）。
- 🚫 不提交 `.DS_Store`、SSH 私钥、口令、token；不混淆本仓库与第三方 `OpenLexington/OpenLexington`（本仓库归属 **CHEN4042**）。
- 🚫 不要大包大揽一次性实现：当前阶段"文档只做参考、逐步推进"，先对齐再动手。

---

## 8. 项目背景 · Context for Continuation

- **项目是什么**：面向**唯一用户本人**的 AI 陪伴助手 —— "一个倾听的、共情的、温柔的、永不忘记你的存在"。核心价值是**情绪陪伴 + 深度交互**，记忆是灵魂。
- **命名**：原名 J.A.R.V.I.S. → 现名 L.E.X.I.N.G.T.O.N.（列克星敦，LEK-sing-tun）；工程代号 `openlexington`（对齐仓库）；AI 自称**列克星敦 / L.E.X.I.N.G.T.O.N.**（中英均可），**不设短缩写与小名**；技术文档保持客观，行文尊重设定。
- **设计灵感**：HomeRail（语音优先 + 生成式 UI + DAG 可追溯）；参考不抄代码：OpenJarvis（本地/隐私优先）、jarvis-ai-platform（分层思路，Spring AI 实现）。
- **交互形态**：语音为主（半双工点按说话，首版）、文本兜底；前端由 AI 全权开发，用户不写前端。
- **开发节奏**：6 个月 / Phase 1–9（§12），每 Phase 留 10–20% 缓冲；本地 LLM 为 Ollama + Qwen2.5，正式待选（通义 / DeepSeek / GLM）。
- **环境事实**：macOS + zsh；GitHub 账号 CHEN4042；开发在 2 台 macOS 间进行，文档**不记录本机绝对路径/用户名**（防公开泄露）。
- **沟通偏好**：中文沟通（技术名词可用英文），回答保持中文；提交习惯：直接 push `main`，无 PR 流程（但**须先获用户指示**）。

---

## 9. 验证命令 · Verification Commands

```bash
# 仓库状态与历史（在仓库根目录执行）
git status && git log --oneline

# SSH 认证（应输出 Hi CHEN4042! ...）
ssh -T git@github.com

# 远端确认（应为 git@github.com:CHEN4042/OpenLexington.git）
git remote -v

# 文件清单（应含 docs/：README + 01）
ls -1 && ls docs/
```

---

## 10. 关键文件 · Key Files

| 文件 | 作用 |
| :--- | :--- |
| `handoff.md` | 本文件：会话交接入口，先读它 |
| `L.E.X.I.N.G.T.O.N. 项目需求说明书 v1.2.md` | **核心规格**：命名体系 / 功能 / 技术选型 / 架构 / 记忆 / 里程碑 / 风险 |
| `L.E.X.I.N.G.T.O.N. 系统架构图 v2.0.md` | PlantUML 源码：AgentScope 运行时版架构图 |
| `README.md` | 项目门面：Logo 与状态徽章 |
| `docs/README.md` | docs 档案索引（编号规则 `NN-主题.md`、只追加不改写） |
| `docs/01-选型-AgentScope-vs-SpringAI-2.0.md` | **选型分析（✅ 定稿）**：AgentScope 2.0 胜出 + 版本策略 |
| `.gitignore` | 忽略规则（含 `.DS_Store`） |

> 注：docs/02（既往 Spring AI 自研实证）已于 2026-09-04 移出仓库并本地归档，不入库。

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

**Spring AI 阵营（对照组）**
- 2.0.0 GA：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now ；2.0.1：https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now
- SAA vs AgentScope 定位（官方团队博客）：http://java2ai.com/en/blog/saa-agentscope-announcement/

---
**End of Handoff** — 下一次会话结束时，请按第 6 节刷新本文件；git 操作须先获用户明确指示。
