# P1 Engineering Foundation

> 状态：IMPLEMENTED — awaiting Cycle Review
>
> 日期：2026-10-03
>
> Cycle：`openalice-20261016`
> 基线：`ef7ed290665eaf9b392d86cb4c227efe65e05aae`

## Goal

建立第一版可运行、可测试的单模块工程基础，打通 Java 21、Spring Boot、SQLite、Flyway、Spring JDBC、Bare AgentScope `ReActAgent`、HTTP/SSE 与 CI，同时保持 OpenAlice 对产品语义和 Execution lifecycle 的所有权。

## Included

- Maven Wrapper、Java 21、Spring Boot 3.5.6 与 GitHub Actions；
- `OPENALICE_HOME/data/openalice.db` 与统一 SQLite DataSource；
- WAL、foreign keys、busy timeout、Flyway V1 migration 与 `JdbcClient`；
- 最小 `Execution` 状态记录以及 `RUNNING / COMPLETED / FAILED / CANCELLED / INTERRUPTED`；
- OpenAlice `AgentRuntime` boundary 与 AgentScope Java 2.0.3 Bare `ReActAgent` adapter；
- execution-scoped AgentScope state、OpenAI-compatible model connection 与 configurable header transport；
- 独立 runtime subscription、Reactor sink 与 WebFlux SSE delivery adapter；
- deterministic model、runtime isolation、SQLite、migration、SSE disconnect 与 context-start tests。

## Excluded

- 完整 Conversation、Message、Turn lifecycle 与 chat endpoint；
- Persona、Character selection、Context Engine、Memory Engine 与 retrieval；
- Tool platform、SubAgent product flow、background/proactivity、Web UI、Voice 与 WebSocket；
- PostgreSQL、Redis、Docker、deployment 与真实模型 CI。

## Implementation choices

- 单 Maven module，能力代码位于 `io.openalice.execution`，runtime adapter 位于 `io.openalice.execution.runtime`；未创建未使用的 `conversation`、`character`、`context` 或 `memory` placeholder。
- `OpenAliceProperties` 统一绑定 home、database 与 model 配置；真实 API key 只从环境变量或 gitignored local config 注入。
- 唯一 DataSource bean 解析 `OPENALICE_HOME/data/openalice.db`，通过 Xerial configuration 对每个 connection 应用 WAL、foreign keys 与 5000 ms busy timeout。
- V1 migration 只创建当前 coordinator 使用的最小 `executions` table；状态更新通过 `JdbcClient` 的 conditional transition 实现 terminal gate。
- `AgentScopeRuntime` 为每个 Execution 创建新的 Bare `ReActAgent`、`InMemoryAgentStateStore` 与 execution-scoped session id；Conversation identity 不进入 AgentScope session semantics。
- model connection 支持 alias、base URL、model name、API key、headers、connect timeout 与 read timeout。自定义 transport 只负责通用配置 header，不包含 provider-specific 产品逻辑。
- `ExecutionCoordinator` 持有 runtime subscription 和内存事件 sink；SSE subscriber 只观察事件，断连不会 dispose runtime subscription。

## Validation

完成前执行并记录：

```bash
./mvnw test
./mvnw package
OPENALICE_HOME=<temporary-directory> ./mvnw spring-boot:run
```

测试不得读取真实模型 key、访问模型网络或写入用户的 `~/.openalice`。

2026-10-03 实际结果：

- `./mvnw test`：PASS，9 tests，0 failures / errors / skipped；
- `./mvnw package`：PASS，9 tests 再次通过并生成可执行 Spring Boot JAR；
- temporary `OPENALICE_HOME` + random port `spring-boot:run`：PASS；
- SQLite 测试同时验证 WAL、foreign keys、5000 ms busy timeout、migration idempotency 与 `VACUUM INTO` snapshot restore；
- dependency tree 只包含 Spring Boot 3.5.6、Flyway 11.7.2、Xerial 3.53.4.0、AgentScope core / OpenAI extension 2.0.3 等已批准依赖，不包含 Harness、Spring AI、JPA、Hibernate 或 MyBatis。

## Completion criteria

- [x] Java 21 单模块工程与 Maven Wrapper；
- [x] SQLite 位于统一解析的 `OPENALICE_HOME`；
- [x] WAL、foreign keys、busy timeout 与 Flyway migration 自动验证；
- [x] Spring JDBC / `JdbcClient` 最小 persistence adapter；
- [x] Bare `ReActAgent` 位于 OpenAlice `AgentRuntime` 后；
- [x] runtime state 按 Execution 隔离；
- [x] OpenAI-compatible model 与 custom header transport seam；
- [x] SSE delivery 与 runtime lifecycle 解耦；
- [x] deterministic tests、random-port HTTP test 与 Java 21 CI；
- [x] 未实现完整 product vertical slice。

## Open questions

- 完整 vertical slice 开始前确定 in-memory runtime event stream 的 retention / eviction policy；当前只保留每个 Execution 最近 64 个非持久事件。
- 在正式 Conversation API 中确定 SSE wire payload、鉴权和错误映射；当前 endpoint 只验证 foundation transport boundary。
- 首次连接目标 provider 时执行显式 opt-in 的 real-model smoke；默认 test 与 CI 不访问模型网络。
- Cycle Review 后决定最小 SQLite backup/restore operational command 与 cadence。

未发现需要修改 Architecture Authority 的 implementation evidence。
