# OpenAlice

OpenAlice 正在进行 Repository Re-initialization。当前目标不是继续堆叠旧实现，而是建立一个适合人和 Coding Agent 长期共同维护的项目基础。

## 当前状态

- 业务实现暂停。
- legacy implementation 与旧构建文件已从 active repository 移除，可通过 Git 历史和已推送的 `bd4618f` 恢复。
- `temp/` 只保存旧产品和工程参考材料，不包含业务实现，也不是当前权威。
- 当前活动计划：[Repository Re-initialization](docs/plans/active/repository-reinitialization.md)。
- 当前交接：[handoff.md](handoff.md)。
- 文档地图：[docs/index.md](docs/index.md)。

产品和架构重新初始化完成前，不开始新的业务功能实现。当前阶段的产出是权威文档、研究边界、架构契约、评测策略和工程协作护栏。

## 目录

```text
docs/       当前项目知识、计划、研究和质量文档
temp/       旧项目的临时参考文档与材料，完成重初始化后删除
```

`temp/` 中的内容不是产品或技术权威；任何重新采用的结论必须写入 `docs/product/` 或 `docs/architecture/`。

产品和架构重新初始化完成前，不创建新的业务源码、构建文件或替代工程骨架。

## 本地安全

真实模型 key 只能放在 gitignored 的本地配置或运行环境中。禁止提交或打印 `application-local.yml`、token、secret、日志和构建产物。
