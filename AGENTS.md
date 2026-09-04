# AGENTS.md · OpenLexington（L.E.X.I.N.G.T.O.N. 列克星敦）

> 本文件给所有在本仓库工作的 Codex / 协作者作为入口指引；运营细节、红线与交接进度以 `handoff.md`（仓库根目录）为准。
> 当前阶段：**assessment-done** —— 文档与 AgentScope 框架评估已完成；**尚无任何代码目录，Phase 1 未启动**。

## 目录结构

```
OpenLexington/
├── README.md            # 项目首页：简介 + 文档导航
├── AGENTS.md            # 本文件：Codex 入口指引（须留在仓库根目录）
├── handoff.md           # 会话交接（每次会话结束刷新；固定在仓库根目录）
└── docs/                # 归档文档
    ├── index.md         # 文档总索引（含每份文档概括；先读它，按需选读）
    ├── 项目需求说明书 v1.2.md  # 核心规格（文件名保留版本号；版本以正文头部为准）
    ├── 系统架构图 v2.0.md      # 系统架构图（PlantUML 源码）
    ├── CONTEXT.md       # 共享语言与术语速查
    └── decisions/       # 技术决策记录（ADR，只追加不改写）
        └── 01-agentscope-vs-springai.md
```

## 阅读顺序（每次开工先读）
1. `handoff.md` —— 会话交接入口（先读；会话结束时更新它）
2. `docs/index.md` —— 文档总索引（含每份文档的概括与「何时读」；**按需选读，不要全量通读所有文档**）
3. 按任务需要从索引挑读：`docs/项目需求说明书 v1.2.md`（核心规格）/ `docs/系统架构图 v2.0.md`（架构）/ `docs/decisions/`（决策）/ `docs/CONTEXT.md`（术语速查）

## 文档权威层级
- 需求以《项目需求说明书》（`docs/项目需求说明书 v1.2.md`）为准；**技术选型 / 评估结论以 `docs/decisions/` 最新决策为准**（含版本结论，例如 AgentScope 采用 2.0.2、遇流式回归回退 2.0.0）。
- 与《需求书》冲突时：以 `docs/decisions/` 最新结论为准，并**知会用户回写**需求书对应章节；不擅自改写历史结论。
- 命名体系（§0，现为双层：工程代号 openlexington / 全称 L.E.X.I.N.G.T.O.N.）、人格、发音兜底三件套、致敬声明 = 项目灵魂，**无用户明确指示不改动**。

## 当前约束（红线精简版，详见根目录 handoff.md §7）
- **只做文档 / 评估**：没有用户明确指令，不创建 Maven 模块、`web/`、`persona/` 等代码目录；不一次性大包大揽，先对齐再动手。
- 技术预选 ≠ 已定架构：AgentScope 尚未实测，可能回退 2.0.0 或双轨。
- 编号不混用：记忆层级 **M1–M3**；隐私分级 **L1/L2**；功能优先级 **P0/P1/P2/暂缓**。
- 不提交 `.DS_Store`、SSH 私钥、口令、token；不混淆本仓库与第三方同名仓库（本仓库归属 CHEN4042）。
- 文档组织约定：`handoff.md` 固定在仓库根目录；正文文档统一放 `docs/` 并用中文描述文件名（`项目需求说明书 v1.2.md` / `系统架构图 v2.0.md` / `CONTEXT.md`；需求 / 架构文件名保留版本号，版本以正文头部为准）；新决策在 `docs/decisions/` 追加 `NN-主题.md`，只追加、不重排旧序号。

## 公开安全（默认按 public 维护）
- 仓库当前 private、未来可能 public：**所有入库内容一律视为可公开**。
- 禁止出现：组织名称与标识、内部项目 / 仓库 / 部署细节、个人邮箱（含工作邮箱）、本机绝对路径与用户名、SSH 细节、密钥 token。
- 提交身份只用 GitHub 账号（noreply 优先）；转 public 前必须先重写 git 历史（历史含旧敏感内容）。

## 沟通与行文
- 中文沟通（技术名词可英文）；本产品面向唯一用户本人，行文尊重设定（AI 自称列克星敦 / L.E.X.I.N.G.T.O.N.，中英均可），技术文档保持客观。
- 命名双层体系（§0）：工程代号 `openlexington`（仓库 / 工程根 / 工程标识前缀，预选）/ 全称 L.E.X.I.N.G.T.O.N.（列克星敦；品牌、人设与 AI 自称，中英均可）。

## 验证命令（仓库根目录执行）
```bash
git status && git log --oneline        # 状态与历史
ssh -T git@github.com                  # 应输出 Hi CHEN4042!
git remote -v                          # 应为 git@github.com:CHEN4042/OpenLexington.git
ls -1 && ls docs/ && ls docs/decisions # 文件清单（根目录含 handoff；docs/ 五项 + decisions/01）
# 提交前脱敏扫描（启发式，模式按需补充，勿写死组织关键词）
grep -rniE "/Users/|[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}|api[_-]?key|secret|password|BEGIN .*PRIVATE" --include="*.md" . || true
```
