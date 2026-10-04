# OpenAlice

OpenAlice 正在进行 Repository Re-initialization。P1 Engineering Foundation 已建立，完整 Conversation vertical slice 尚未开始。

## 当前状态

- Java 21、Spring Boot、SQLite、Flyway、原生 MyBatis Mapper、AgentScope runtime boundary、HTTP/SSE、自定义日志与统一异常框架、测试及 CI 已建立。
- legacy implementation 与旧构建文件已从 active repository 移除，可通过 Git 历史和已推送的 `bd4618f` 恢复。
- `temp/` 只保存旧产品和工程参考材料，不包含业务实现，也不是当前权威。
- 当前活动计划：[P1 Engineering Foundation](docs/plans/active/p1-engineering-foundation.md) 与 [Repository Re-initialization](docs/plans/active/repository-reinitialization.md)。
- 当前交接：[handoff.md](handoff.md)。
- 文档地图：[docs/index.md](docs/index.md)。

当前工程只提供下一轮 vertical slice 所需的基础，不包含完整聊天产品行为。

## 目录

```text
docs/       当前项目知识、计划、研究和质量文档
src/        P1 Engineering Foundation 源码与测试
temp/       旧项目的临时参考文档与材料，完成重初始化后删除
```

`temp/` 中的内容不是产品或技术权威；任何重新采用的结论必须写入 `docs/product/` 或 `docs/architecture/`。

## 本地验证

需要 JDK 21。基础测试不需要模型 API key，也不会访问真实模型：

```bash
./mvnw test
```

应用默认把运行数据写入 `~/.openalice/`。本地 smoke 可使用临时目录：

```bash
OPENALICE_HOME="$(mktemp -d)" ./mvnw spring-boot:run
```

只有发起真实 Agent execution 时才需要通过环境变量提供 OpenAI-compatible model 配置；变量名见 [`.env.example`](.env.example)。

## 本地安全

真实模型 key 只能放在 gitignored 的本地配置或运行环境中。禁止提交或打印 `application-local.yml`、token、secret、日志和构建产物。
