# Handoff · OpenLexington（L.E.X.I.N.G.T.O.N. 列克星敦）

> 本文件是**给下一位接手本项目的 AI / 工程师的会话交接文档**（session handoff）。
> 接手顺序：**先读本文件** → 再读《项目需求说明书 v1.2》→ 需要架构细节时读《系统架构图 v2.0》。
> 维护约定：每次会话结束（换人 / 换会话 / 上下文耗尽前）更新本文件；宁可整体重写，也不叠床架屋。

## 元信息 · Metadata

| 项目 | 值 |
| :--- | :--- |
| **Written** | 2026-09-03（v1 · 本仓库首份 handoff） |
| **Status** | `at-checkpoint` —— 文档阶段完成并已推送；AgentScope 架构评估**尚未启动** |
| **Branch** | `main` |
| **Last commit** | `be86733` docs: 修正需求说明书日期/记忆层级编号/VAD表述，并忽略 .DS_Store |
| **Remote** | `git@github.com:CHEN4042/OpenLexington.git`（SSH，可用） |
| **Previous handoff** | 无 |

---

## 1. 本次任务 · Task

本次会话（2026-09-03）共四件事，均已完成：

1. 定位并清理误删后遗留的 OpenLexington 仓库残余；
2. 修复 GitHub SSH 认证并完成首次 clone / push；
3. 通读用户放进去的两份设计文档并**仅修正其中的文档错误**（技术选型、目录结构一律未动）；
4. 撰写本交接文档（用户要求：参考网络规范 + GitHub 大项目真实案例）。

---

## 2. 当前状态 · Current State

- 仓库在 `/Users/haochen/IdeaProjects/OpenLexington`，`main` 分支，工作区干净，已同步远端。
- 仅有 4 个文件：`README.md`（门面）、需求说明书 v1.2（543 行）、系统架构图 v2.0（PlantUML 源码）、`.gitignore`。
- **尚无任何代码目录**（无 Maven 模块、无 `web/`、无 `docs/`、无 `persona/`）——这是用户明确的阶段性决定，不是遗漏。
- 用户口径：**文档目前只作参考**；项目具体架构要**等后续评估 AgentScope 之后再定**；其他文件夹等评估后再创建。

---

## 3. 已完成工作 · What Was Done

### 3.1 环境修复：GitHub SSH 认证 ✅

| 项 | 详情 |
| :--- | :--- |
| 问题 | `~/.ssh/config` 中 github.com 指向 `id_rsa`（**未注册**到 GitHub），认证失败 |
| 修复 | 改为 `~/.ssh/id_ed25519`（已注册到账号 CHEN4042）；原配置备份于 `~/.ssh/config.bak` |
| 验证 | `ssh -T git@github.com` → `Hi CHEN4042! You've successfully authenticated...` |

> 注意：SSH 配置在 `~/.ssh/`，**不在 git 仓库内**，换机器需重新配置。SSH 私钥任何情况下不得入库。

### 3.2 文档勘误（commit `be86733`）✅

仅修错误，未改任何技术选型 / 范围 / 目录规划：

| # | 修正点 | 位置 | 说明 |
| :- | :--- | :--- | :--- |
| 1 | 未来日期 `2026-10-13` → `2026-09-03` | 需求说明书页头 + §17 版本历史 | 文档日期晚于"最后更新"，自相矛盾 |
| 2 | 记忆层级 `L1–L3` → **`M1–M3`** | §5.1 引用、§8 表格、新增编号约定 | 与 §3.3 **隐私分级 L1/L2** 编号撞车，易混用；M = Memory |
| 3 | VAD 断句口径统一：**静音 700ms 视为停止说话** | §6.3 | 原文"浏览器判定/服务端判定"自相矛盾；终端归属留待 Phase 2 调研 |
| 4 | `.gitignore` 新增 `.DS_Store` | 仓库根 | macOS 生成文件，防止误提交 |

### 3.3 Git 历史（全部已推送）

```
be86733  docs: 修正需求说明书日期/记忆层级编号/VAD表述，并忽略 .DS_Store
7f49c68 Update README.md
b45e6cb Initial commit
```

### 3.4 走过的弯路 · Dead Ends（下一位勿重蹈）

- **仓库归属误判**：曾把用户自建仓库误解析为第三方 `OpenLexington/OpenLexington`，浪费一轮检索。
  教训：涉及 remote 归属不清时，先 `git remote -v` / `gh repo view` 确认所有者，再动手。
- **`id_rsa` 直连失败**：不是网络问题，是 `~/.ssh/config` 里 IdentityFile 指错了未注册的密钥。
- **凭记忆提交**：第一版日期修正只改页头漏了版本历史表，review 时补全 —— 改文档要全局搜同字段。

---

## 4. 关键决策 · Key Decisions

| 决策 | 理由 | 状态 |
| :--- | :--- | :--- |
| 项目名 J.A.R.V.I.S. → **L.E.X.I.N.G.T.O.N.**（列克星敦），AI 自称 **Lexi** | 用户更名，确立四层命名体系（§0） | ✅ 已定稿（文档） |
| 后端 Agent 框架选 **AgentScope Java 2.0.0**（锁版本） | 2026-07 GA、双层 Agent / 事件流 / 权限 / workspace sandbox；**锁 2.0.0 规避 2.0.1 流式回归** | ⚠️ **仅参考，未评估**（下一位首要任务） |
| 后端 Java 21 + Spring Boot 3 + Spring AI Alibaba；前端 React 18 + Vite + shadcn/ui + Zustand | 官方推荐接入路径；前端选 AI 生成质量最高的栈（用户不写前端） | ⚠️ 仅参考 |
| 记忆三级自研 **M1 Redis / M2 PG / M3 pgvector**，AgentScope Memory 仅作桥接 | "记忆是灵魂，不外包给框架" | ⚠️ 仅参考 |
| 首版范围：单用户、半双工语音 + 文本兜底、Web UI；DAG / 工具 / 主动推送等延后 | 范围控制（§2.2 P0/P1/P2/暂缓） | ✅ 已定稿 |
| 目录结构（Maven 四模块 + web + persona + docs）**暂不创建** | 用户要求等 AgentScope 评估后再建 | 🕐 挂起，待用户确认 |

---

## 5. 未决问题 · Open Issues

- 🔴 **AgentScope Java 2.0.0 尚未做真实 API 评估** —— 需求说明书基于官方文档/宣传撰写，未经验证。Phase 1 依赖的事件流 token 级转发、权限系统、workspace sandbox、MCP、Memory 桥接是否如文档所述，全部待验证。若评估不通过，文档预留了"回退 Spring AI Alibaba 稳定版"双轨。
- 🟡 需求说明书 §15 待定事项（LLM 正式选型、ASR/TTS、云服务商、人格语料、VAD 终端归属）均未调研。
- 🟡 §15 中 "GitHub `<username>` 待定" —— **现已确认为 `CHEN4042`**，未来建工程时 groupId 应为 `io.github.CHEN4042.lexington`，可顺手从待定项移除（需用户确认）。
- 🟢 系统架构图 v2.0 为 PlantUML 源码，尚未实际渲染验证过（低风险，随手可验）。

---

## 6. 给下一位 AI 的下一步 · Next Steps

按顺序执行，每步可独立验证：

1. **通读《L.E.X.I.N.G.T.O.N. 项目需求说明书 v1.2.md》全文**。
   重点：§0 命名体系（四层命名 / 工程命名规范，动手前必读）、§2.2 功能清单（P0 范围）、§4 技术选型、§7.2 Maven 模块结构、§8 记忆 M1–M3、§12 里程碑（Phase 1–9）、§14.1 AgentScope、§15 待定、§16 风险。
2. **评估 AgentScope Java 2.0.0 真实能力**（只读调研，不写代码、不改文档）：
   - 仓库 https://github.com/agentscope-ai/agentscope-java 、文档 https://java.agentscope.io ；
   - 核对：事件流能否支撑 token 级流式 → WebSocket 前向（首 token p95 < 2s）、权限系统 / workspace sandbox 是否满足 §3.2、Memory 抽象能否桥接自研记忆、MCP 工具闭环；
   - 产出**一份评估结论**（建议/风险/与文档假设的差异），与用户讨论后再定架构。
3. **与用户确认后**才创建工程骨架：Maven 四模块（domain / application / infrastructure / server）+ `web/` + `persona/` + `docs/`，groupId `io.github.CHEN4042.lexington`，按 §7.2 依赖方向（六边形架构）。
4. 骨架落成后按 §12 Phase 1 验收项推进（事件流流式输出 + 权限 sandbox）。
5. **会话结束前更新本文件**：刷新 Written / Last commit / 各章节，并附本次 commit hash 供 `git log` 对账。

---

## 7. 红线 · What NOT to Do

- 🚫 **未经用户确认，不要创建任何新目录 / 新文件**（docs/、Maven 模块、web/、persona/ 一律等指令）。
- 🚫 **不要把"技术预选"当"已定架构"**：AgentScope 未评估，随时可能推翻（含双轨回退方案）。
- 🚫 **不要混用编号**：记忆层级是 **M1–M3**；隐私分级是 **L1/L2**（§3.3）。两者维度不同。
- 🚫 **不要提交 `.DS_Store`**（已 ignore）；不要把 SSH 私钥、口令、token 写入仓库。
- 🚫 **不要改动**需求说明书中的命名体系、人格九特质、兜底三件套、致敬声明（含舰R/情感设定，项目灵魂）。
- 🚫 **不要混淆本仓库与第三方 `OpenLexington/OpenLexington`**：本仓库归属 **CHEN4042**。
- 🚫 **不要大包大揽一次性实现**：用户当前阶段明确"文档只做参考、逐步推进"，先对齐再动手。

---

## 8. 项目背景 · Context for Continuation

以下信息无法从代码里直接发现，但对理解项目至关重要：

- **项目是什么**：面向**唯一用户本人**的 AI 陪伴助手 —— "一个倾听的、共情的、温柔的、永不忘记你的存在"。核心价值是**情绪陪伴 + 深度交互**，记忆是灵魂（"绝不允许 AI 忘记和用户之间的故事"）。
- **更名历史**：原名 J.A.R.V.I.S.（钢铁侠管家致敬）→ 现名 L.E.X.I.N.G.T.O.N.（列克星敦，LEK-sing-tun），AI 自称 **Lexi**；名称致敬与人格设定有情感属性，技术文档中保持客观、行文中尊重设定。
- **设计灵感**：HomeRail（语音优先 + 生成式 UI + DAG 可追溯）；参考但不抄代码：OpenJarvis（本地/隐私优先）、jarvis-ai-platform（分层思路，注意其是 Spring AI 实现）。
- **交互形态**：语音为主（半双工点按说话，首版）、文本兜底；前端由 AI 全权开发，用户不写前端。
- **开发节奏**：6 个月 / Phase 1–9（§12），每 Phase 留 10–20% 缓冲；本地开发 LLM 为 Ollama + Qwen2.5，正式待选（通义 / DeepSeek / GLM）。
- **环境事实**：macOS + zsh；用户 GitHub 账号 CHEN4042（chenhao404@icloud.com）；本地密钥 `~/.ssh/id_ed25519`，旧配置备份于 `~/.ssh/config.bak`。
- **沟通偏好**：用户以中文沟通（技术名词可用英文），回答保持中文；提交习惯：直接 push `main`，无 PR 流程。
- **同类本地项目**：`/Users/haochen/IdeaProjects/` 下有 `JARVIS个人陪伴助理`、`Jarvis`、`Persona-Agent-Hub`、`nano-ontoprompt` 等，是该项目的前身 / 相关实验，可作参考但注意版本陈旧。

---

## 9. 验证命令 · Verification Commands

```bash
# 仓库状态与历史
cd /Users/haochen/IdeaProjects/OpenLexington && git status && git log --oneline

# SSH 认证（应输出 Hi CHEN4042! ...）
ssh -T git@github.com

# 远端确认（应为 git@github.com:CHEN4042/OpenLexington.git）
git remote -v

# 文件清单（应只有 README / 两份文档 / .gitignore / handoff.md）
ls -1
```

---

## 10. 关键文件 · Key Files

| 文件 | 作用 |
| :--- | :--- |
| `handoff.md` | 本文件：会话交接入口，先读它 |
| `L.E.X.I.N.G.T.O.N. 项目需求说明书 v1.2.md` | **核心规格**（543 行）：命名体系 / 功能 / 技术选型 / 架构 / 记忆 / 里程碑 / 风险 |
| `L.E.X.I.N.G.T.O.N. 系统架构图 v2.0.md` | PlantUML 源码：AgentScope 运行时版架构图，可渲染验证 |
| `README.md` | 项目门面：双语 Logo 与状态徽章 |
| `.gitignore` | 忽略规则（含 `.DS_Store`） |

---

## 11. 参考资料 · References（本文件的撰写依据）

**GitHub 大项目真实案例（AI 上下文 / 交接文档实践）**

- [vercel/next.js · AGENTS.md](https://github.com/vercel/next.js/blob/canary/AGENTS.md)（`CLAUDE.md` 为同名软链）—— 130k+ star 巨仓的 AI 开发指南：仓库结构 / 构建命令 / 代码规范，先给地图再给规则
- [langchain-ai/langchain · CLAUDE.md](https://github.com/langchain-ai/langchain/blob/master/CLAUDE.md) —— monorepo 架构 + 各包边界说明
- [facebook/react · CLAUDE.md](https://github.com/facebook/react/blob/main/CLAUDE.md) —— 极简（~10 行）：现状 + 当前活跃工作 + 分支
- [openai/codex · AGENTS.md](https://github.com/openai/codex/blob/main/AGENTS.md) —— 仓库级工程规范（含明确禁令清单），红线写法参考
- [yoheinakajima/activegraph · HANDOFF.md](https://github.com/yoheinakajima/activegraph/blob/main/HANDOFF.md)（BabyAGI 作者，626⭐）—— 真实"每会话一换"的 HANDOFF.md：Current State → 纪律模式（Discipline patterns）→ 交接下一步
- [dongsheng123132/u-claw · HANDOFF.md](https://github.com/dongsheng123132/u-claw/blob/main/HANDOFF.md)（1.7k⭐）—— AI 装机工具链项目的现役 handoff
- [tw93/Luo · HANDOFF.md](https://github.com/tw93/Luo/blob/master/HANDOFF.md) —— 中文、版本化（v0.4.x）的技术交接：根因诊断 + 修复内容 + 度量表 + 视觉验证，叙事干净

**规范 / 模板**

- [wjgoarxiv/agent-handoff-skill](https://github.com/wjgoarxiv/agent-handoff-skill) —— 7 必备章节（Task / Current State / What Was Done / Key Decisions / Open Issues / Next Steps / Context）+ `[UNVERIFIED]` 标记 + 现有文件"宁可整体替换"策略
- [timothyjrainwater-lab/multi-agent-coordination-framework](https://github.com/timothyjrainwater-lab/multi-agent-coordination-framework) —— 简洁版：附 commit hash 可对账、Next Steps 最重要、Files to Read 3–5 个即可
- [willseltzer/claude-handoff](https://github.com/willseltzer/claude-handoff) —— "Show code, don't describe"、Failed Approaches 必写、测试步骤带期望输出
- [bestcow/dear-agent](https://github.com/bestcow/dear-agent) —— 5 文件 docs 约定，`HANDOFF.md` 在**每个会话结束时**更新（含 status frontmatter）

**官方文档**

- [Anthropic · Claude Code Memory（CLAUDE.md / AGENTS.md 最佳实践）](https://code.claude.com/docs/en/claude-md)

---
**End of Handoff** — 下一次会话结束时，请按第 6 节第 5 条刷新本文件。
