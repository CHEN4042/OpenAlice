# AGENTS.md · OpenAlice（A.L.I.C.E. 爱丽丝）

> 本文件给所有在本仓库工作的 Codex / 协作者作为入口指引；运营细节、红线与交接进度以 `handoff.md`（仓库根目录）为准。
> 当前阶段：**phase1-implementation** —— 根目录四模块骨架与最小可运行链路已创建。

## 目录结构

```text
OpenAlice/
├── README.md
├── AGENTS.md
├── handoff.md
├── pom.xml
├── openalice-core/       # 领域模型与端口
├── openalice-memory/     # 自研记忆实现
├── openalice-agent/      # AgentScope runtime
├── openalice-server/     # Spring Boot HTTP 壳与组合根
├── web/                  # 前端占位
└── docs/
    ├── index.md
    ├── 项目需求说明书 v1.2.md
    ├── CONTEXT.md
    └── decisions/
```

## 阅读顺序（每次开工先读）

1. `handoff.md`
2. `docs/index.md`
3. 按任务需要挑读：ADR 06（当前布局）、ADR 05（架构蓝图与规范）、`docs/CONTEXT.md`

## 文档权威层级

- 需求以《项目需求说明书》为核心参考；技术选型 / 架构结论以 `docs/decisions/` 最新决策为准。
- ADR 06 修订 ADR 05 的 `service/` 顶层布局；ADR 05 的模块边界、开发规范、测试策略与扩展预留仍有效。
- 参考项目与需求书只作参考，不约束最终实现。

## 当前工程规则

- Java 17，Maven 多模块，根目录 `pom.xml` 聚合四个模块。
- AgentScope Java 2.0.2 是预选底座；Spring Boot 3.5.16 仅作 Web 壳。
- 不引入 Spring AI / Spring AI Alibaba。
- `openalice-agent` 不依赖 `openalice-memory`，只依赖 `openalice-core` 的 `MemoryPort`。
- `openalice-server.composition` 是唯一组合根。
- `web/` 不进入 Maven Reactor。
- Phase 1 使用 deterministic model 与 in-memory memory，不接外部 LLM API。
- 语音、learning、插件、多 Agent 只保留架构位置，不提前创建空模块。

## 开发规范

- 遵循 Maven / Spring 官方多模块建议与 Alibaba P3C / Google Java Style 中不冲突的部分。
- `openalice-core` 只放领域对象、端口与纯 POJO，不允许出现 `impl` 包或框架依赖。
- 测试分层：`core` 纯单元测试；`memory` 存储测试；`agent` 用 fake `MemoryPort` 且不得依赖 `openalice-memory`；`server` 做集成测试。
- Agent 测试必须覆盖不同 session 的上下文隔离。
- 公共 API 与核心端口变更需同步更新 ADR / handoff。

## 公开安全

- 仓库按 public 安全标准维护。
- 禁止提交组织名称、内部项目细节、个人邮箱、本机绝对路径、用户名、SSH 私钥、token。
- 禁止提交 `.idea/`、`.DS_Store`、日志、构建产物。

## Git 协作规范

- **commit：AI 负责本地提交**，但必须等用户明确指示或 review 确认后执行。
- **push：一律由用户手动执行**。Codex 不执行 `git push`。
- message 遵循 Conventional Commits：`<type>(<scope>): <中文 subject>`。

## 常用命令

```bash
mvn clean test
mvn -pl openalice-server -am package
mvn -pl openalice-server spring-boot:run
```

## 沟通

- 中文沟通，技术名词可用英文。
- 产品面向唯一用户本人；AI 自称爱丽丝 / Alice。
- 不把 OpenAlice / A.L.I.C.E. 解释为字母递归人格。
