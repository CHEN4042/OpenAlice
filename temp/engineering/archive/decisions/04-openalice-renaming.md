# 04 · OpenAlice 更名决议（OpenLexington → OpenAlice）

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-06 |
| 性质 | **命名决议（ADR）** —— 记录项目更名 OpenAlice 的决议、词形映射与删除清单 |
| 状态 | ✅ 已定稿（v10 / 需求书 v1.3，待提交） |
| 关联 | [01 · AgentScope Java 2.0 vs Spring AI 2.0 选型分析](./01-agentscope-vs-springai.md)（框架选型，不受更名影响）；《需求说明书》§0（v1.3） |

---

## 1. 背景

- 2026-09-06 用户指示：项目更名 **OpenLexington → OpenAlice**。
- 用户说明：命名不再承担"人格说明书"职能（删除递归展开九特质）；取名回归 Alice 本身；A.L.I.C.E. 为仪式 / 人设形态（点分大写），中文名 / AI 自称**爱丽丝**。

## 2. 词形映射（全仓同步：当前状态一律用新词形）

| 旧 | 新 | 说明 |
| :--- | :--- | :--- |
| `openlexington` | `openalice` | 工程代号（小写词根：Maven / 包名 / npm / Docker / Redis / 环境变量 / 日志前缀，预选） |
| `OpenLexington` | `OpenAlice` | GitHub 仓库名 / 工程代号首字母大写形态 |
| `L.E.X.I.N.G.T.O.N.` | `A.L.I.C.E.` | 品牌 / 人设形态（点分大写，仅仪式 / Logo 呈现，**非首字母缩写**） |
| 列克星敦 | 爱丽丝 | 中文名 / AI 自称 |
| `LEK-sing-tun` | `/ˈælɪs/` | 注音 |
| `OPENLEXINGTON_` | `OPENALICE_` | 环境变量前缀 |

## 3. 删除清单（命名瘦身）

- 全称递归展开为九特质（Listening / Empathetic / Xenial / Intimate / Nurturing / Growing / Trustworthy / Open-hearted / Navigator）；
- 字母 ↔ 需求条目映射表与"名字即人格说明书"表述；
- 致敬战舰少女R / USS Lexington / lex- 词根彩蛋 / 海军术语双关；
- README 特质展开行；
- 人格设定中"九特质版本"标签与"Navigator 特质"引用（改"生活领航"表述）；
- "九特质开关"表述（改"人格维度开关"）。

## 4. 灵感来源（新，用户 2026-09-06 提供）

- **主灵感**：碧蓝档案（Blue Archive）· 爱丽丝；
- **致敬**：《爱丽丝梦游仙境》（Alice's Adventures in Wonderland）；
- **呼应**：Red Queen（"红后"，虚构作品中"初代强 AI"意象）。

## 5. 无关联声明

本项目为个人非商业自用项目，与 GitHub 上任何同名 / 近似项目（如 `TraderAlice/OpenAlice` 等）**无任何关联**；名字仅为个人情感致敬，与碧蓝档案 / 《爱丽丝梦游仙境》版权方无关；如未来开源，README 保留致敬与声明。

## 6. 落地范围

- 《项目需求说明书》升 **v1.3**（§17 只追加新版本行，不改旧行）；
- 全仓"当前状态"表述同步新词形（README / AGENTS / CONTEXT / index / 架构图 / handoff v10）；
- **历史记录保留旧名不改写**：git 历史 commit message、各文档版本历史旧行、handoff §1 旧 v 段 / §3 记录、index 整理记录旧行；
- decisions/01、02 中指向本项目的代称同步为 OpenAlice（评估结论不改写）。

---

*本文档为命名决策记录；与更名相关的既有历史结论以各文档历史行为准，不追溯改写。*
