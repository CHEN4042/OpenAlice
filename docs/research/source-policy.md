# Research Source Policy

> 状态：ACTIVE · 适用于 Repository Re-initialization 及后续 architecture research。

## 目的

研究用于支持项目判断，不替代项目判断。研究结论必须与 OpenAlice 的产品契约、规模、隐私边界和维护能力结合后，才能进入架构或实现。

## 来源优先级

1. 官方产品文档、官方仓库、标准、规范和维护者发布的设计资料；
2. 原始论文、基准数据集和实验报告；
3. 活跃开源仓库的代码、测试、issue 和维护文档；
4. 具有明确方法和引用链的二手分析；
5. 博客、论坛和搜索摘要只能作为发现线索，不能单独支撑关键决策。

涉及 OpenAI 产品时优先使用官方 OpenAI 文档；涉及安全、隐私、供应商行为或当前版本时必须检查时效性。

## OpenAlice 对标项目调研顺序

研究 OpenAlice 的架构、Agent、Memory、Prompt、Runtime 时，先检查核心产品对标：

1. SillyTavern；
2. OpenHanako / HanaAgent；
3. OpenClaw；
4. Hermes Agent。

然后检查长期形态参考 HomeRail，同时主动寻找目标相近、但更成熟或采用度更高的项目。质量判断结合真实实现、测试、维护和使用证据，不以 star 数作为唯一标准；同名项目须先核对身份。

接着扩展到成熟项目，例如 Agent Zero、OpenHands、Letta、LangGraph、Microsoft Agent Framework / AutoGen、CrewAI，以及其他发现的高质量项目。这些只是发现方向，不代表必须采用。

最后检查相关原始论文、标准、学术研究及 benchmark / evaluation。以上是检查顺序，不改变既有来源等级；优先官方文档、仓库、源码、issue，记录时区分事实、推断与建议。

## 研究记录要求

每份研究文档应记录：

- 日期、问题、范围和检索关键词；
- 来源 URL、标题、发布/更新日期（若可得）；
- 观察到的事实与 OpenAlice 的推断分开；
- 适用条件、反例、成本和未决风险；
- 是否影响产品契约、architecture spine、quality 或 operations；
- 若结论被采纳，指向对应的稳定文档或 ADR。

研究文档放在 `docs/research/`，文件名包含日期。没有经过项目决策确认的研究不能成为代码实现依据。

## 安全与隐私

- 不把真实用户对话、个人资料、密钥、token、日志或本机绝对路径上传到外部研究服务。
- 公开评测使用合成或脱敏数据；真实资料只保存在明确授权的本地/private 环境。
- 引用第三方内容时只保留必要摘录和链接，不复制受版权保护的大段文本。
