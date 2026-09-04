# AGENTS.md · OpenLexington（L.E.X.I.N.G.T.O.N. 列克星敦）

> 本文件给所有在本仓库工作的 Codex / 协作者作为入口指引；运营细节、红线与交接进度以 `handoff.md` 为准。
> 当前阶段：**assessment-done** —— 文档与 AgentScope 框架评估已完成；**尚无任何代码目录，Phase 1 未启动**。

## 阅读顺序（每次开工先读）
1. `handoff.md` —— 会话交接入口（先读；会话结束时更新它）
2. `L.E.X.I.N.G.T.O.N. 项目需求说明书 v1.2.md` —— 核心规格（命名体系 / 功能 / 选型 / 架构 / 记忆 / 里程碑）
3. `L.E.X.I.N.G.T.O.N. 系统架构图 v2.0.md` —— PlantUML 架构源码
4. `docs/README.md` 与 `docs/NN-*.md` —— 决策档案

## 文档权威层级
- 需求以《项目需求说明书》为准；**技术选型 / 评估结论以 `docs/` 最新决策为准**（含版本结论，例如 AgentScope 采用 2.0.2、遇流式回归回退 2.0.0）。
- 与《需求书》冲突时：以 `docs/` 最新结论为准，并**知会用户回写**需求书对应章节；不擅自改写历史结论。
- 命名体系（§0，现为双层：工程代号 openlexington / 全称 L.E.X.I.N.G.T.O.N.）、人格、发音兜底三件套、致敬声明 = 项目灵魂，**无用户明确指示不改动**。

## 当前约束（红线精简版，详见 handoff.md §7）
- **只做文档 / 评估**：没有用户明确指令，不创建 Maven 模块、`web/`、`persona/` 等代码目录；不一次性大包大揽，先对齐再动手。
- 技术预选 ≠ 已定架构：AgentScope 尚未实测，可能回退 2.0.0 或双轨。
- 编号不混用：记忆层级 **M1–M3**；隐私分级 **L1/L2**；功能优先级 **P0/P1/P2/暂缓**。
- 不提交 `.DS_Store`、SSH 私钥、口令、token；不混淆本仓库与第三方同名仓库（本仓库归属 CHEN4042）。

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
ls -1 && ls docs/                      # 文件清单（应含 docs/README + 01）
# 提交前脱敏扫描（启发式，模式按需补充，勿写死组织关键词）
grep -rniE "/Users/|[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}|api[_-]?key|secret|password|BEGIN .*PRIVATE" --include="*.md" . || true
```
