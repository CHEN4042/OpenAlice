# P1 Engineering Foundation 调研

> **状态：WORKING NOTE / NON-AUTHORITATIVE**<br>
> **日期：2026-10-02**<br>
> **范围：** SQLite / PostgreSQL、HTTP + SSE / WebSocket、P1 最小 durable Conversation / Turn / Execution state。<br>
> **不包含：** 最终 Schema、应用实现、AgentScope Technical Spike、Memory / Background / Persona 等既有研究范围。<br>
> 本文遵守 [Research Source Policy](source-policy.md)，为 User + Web review 提供证据，不直接建立 Architecture Authority。

## 1. Executive conclusion

| Area | Recommendation | Confidence | Status |
| :-- | :-- | :-- | :-- |
| P1 Database | SQLite，单机文件存储；使用当前已修复 WAL 问题的版本、WAL、短事务与显式备份 | High | CANDIDATE |
| P1 Transport | 普通 HTTP command/query + server-to-client SSE；取消使用独立 HTTP command | High | CANDIDATE |
| Web stack | P1 可用 Spring MVC `SseEmitter` 或 MVC reactive return；不因 AgentScope 使用 Reactor 就强制 WebFlux | Medium | CANDIDATE |
| Durable state | Conversation、Message、Turn、Execution 四个 identity 均持久化，但只保存最小产品事实与终态 | High | CANDIDATE |
| AgentScope mapping | AgentScope session / `AgentState` / interrupt 仅作 runtime state；OpenAlice identity 与状态是事实来源 | High | ACCEPTED boundary / mapping NEEDS PROTOTYPE |

这三个建议没有改变现有 accepted decisions；它们关闭或收窄了 Architecture Synthesis 中的 P1 database、transport 与 execution-state OPEN 项。

## 2. 方法、环境与证据边界

本轮只针对以下实际环境判断：单用户、Java 21、Spring Boot、单 backend process、MacBook 开发、未来同机常驻 Mac mini、一条永久 Conversation、P1 durable Message / Turn / Execution，以及 P2+ 可能出现的 Memory / retrieval。

资料访问于 2026-10-02。数据库与 Spring 行为以官方文档为主；Java 驱动、pgvector 与 AgentScope 行为使用官方仓库的固定 release/tag。关键固定版本是：

- Xerial SQLite JDBC `3.53.4.0`，release commit `cab7981`，内含 SQLite 3.53.4；
- pgvector `v0.8.6`；
- AgentScope Java `v2.0.3`，release commit `1b8e3dc`。

AgentScope `main` 在 release 后仍持续变化，因此本文不把 `main` 新增 API 当作 P1 已确认能力。未运行外部项目或本仓库代码；所有 runtime 行为结论仍受后续 prototype 限制。

## 3. Question A — SQLite vs PostgreSQL

### 3.1 Observed facts

**SQLite deployment and concurrency**

- SQLite 官方选择清单认为：数据与 application server 位于同一设备、写并发低、数据远小于 TB 时，SQLite 通常合适；需要跨网络直接访问数据、多个并发写者或多服务器时，应改用 client/server database。[S1]
- WAL 允许 reader 与 writer 并行，但同一数据库仍只有一个 writer。WAL 依赖同机 shared memory，不能放在 network filesystem；checkpoint 也需要运维关注。[S2]
- SQLite 官方在 2026-03 公布并修复 WAL-reset corruption bug：受影响范围到 3.51.2，3.51.3 及以后修复。因此 P1 若使用 WAL，不能采用旧系统自带版本；应由 JDBC dependency 固定已修复版本。[S2]
- SQLite Online Backup API 与 `VACUUM INTO` 都能生成一致 snapshot；直接只复制主 `.db` 文件在 WAL 活跃时不安全，WAL 是数据库持久状态的一部分。[S2][S3]
- SQLite 原生提供 JSON functions/operators；Xerial JDBC 将 macOS arm64/x86_64 等 native library 打包进单一 JAR，并内置 JSON、FTS5 等官方 extension。[S4][S5]

**PostgreSQL deployment and capabilities**

- PostgreSQL 用 MVCC 使普通读写互不阻塞，并提供 row/table/advisory locks；这比 SQLite 的单 writer 更适合持续的并发写入。[S6]
- PostgreSQL `jsonb` 是可索引的结构化二进制表示，适合频繁查询 metadata。`pg_dump` 产生一致 snapshot，可跨 server version / machine architecture 恢复。[S7]
- pgvector `v0.8.6` 在 PostgreSQL 13+ 提供 exact search、HNSW 与 IVFFlat；approximate index 需要在 recall、build time、memory 与 query tuning 之间做真实评测。[S8]

**Java / Spring integration**

- Spring Boot 支持标准 `DataSource`、JDBC / `JdbcClient` / `JdbcTemplate`、Spring Data JDBC 与 JPA。PostgreSQL 有 Boot 可直接构造的 `PGSimpleDataSource` 和 Hibernate core 支持的 `PostgreSQLDialect`。[S9][S10]
- Xerial 提供成熟 SQLite JDBC，但 SQLite 不在 Spring Boot 自动识别的 embedded database 清单中，需要显式 DataSource 配置。Hibernate 将 `SQLiteDialect` 放在 best-effort 的 `hibernate-community-dialects`，不由 Hibernate team CI 或 support 直接保障。[S5][S9][S10]
- Flyway 同时支持 SQLite 与 PostgreSQL。SQLite driver reference 明确限制 concurrent migration、schema 与 nested transaction；这对单进程启动 migration 可接受，但不应假装两种数据库的 DDL 完全可移植。[S11][S12]

### 3.2 Interpretation for OpenAlice P1

OpenAlice 的 browser、phone 与未来 background completion 都通过同一个 backend process 写库，不会各自直接打开数据库文件。P1 的 write transaction 主要是：保存 User Message / Turn / Execution、提交 assistant outcome，以及更新终态。只要 LLM / Tool 调用不占用数据库 transaction，单次写锁应保持在毫秒级。SQLite 的一个 writer 因此是现实约束，但不是当前 blocker。

PostgreSQL 的并发、JSONB、成熟 ORM 与未来 pgvector 路径更强；代价是 P1 从第一天就需要安装和守护数据库服务、管理 role/database、备份恢复、升级和连接配置。对一个同机、单用户、单进程产品，这些成本暂时没有对应的 P1 product value。

SQLite 的 Spring/JPA 支持层级弱于 PostgreSQL。这个事实支持 P1 使用小而明确的 JDBC / Spring JDBC persistence adapter 和 versioned SQL migration，而不是为追求 ORM portability 依赖 community dialect。它并不要求把 SQL 泄漏到 product semantics。

### 3.3 Reliability and operating profile

SQLite candidate 只有在以下运行约束同时成立时才可靠：

- 数据库文件位于 backend 所在 Mac 的本地文件系统，不放在 NAS、iCloud Drive 或 network filesystem；
- 固定 SQLite 3.51.3+；当前 Xerial 3.53.4.0 满足已知 WAL-reset fix；
- 启用 foreign keys、合理 busy timeout 与 WAL；持久化产品消息优先采用能承受 machine restart / power loss 的 synchronous policy，P1 不为微小吞吐牺牲 durability；
- LLM、Agent 与 Tool 执行发生在 transaction 之外；transaction 只做短小状态转换；
- connection pool 保持小规模，并把 `SQLITE_BUSY` 当作可观察错误处理，不靠无限重试掩盖长事务；
- backup 使用 SQLite-aware snapshot mechanism，并实际做 restore test；不能只复制一个可能与 WAL 分离的主文件。

具体 PRAGMA、pool size 与 backup schedule 是实现期配置，本文不固化数值。

### 3.4 P2 Memory and migration pressure

引入 semantic Memory 不自动要求 PostgreSQL。真正压力来自：

1. 已确认需要 vector search，并希望 embedding 与关系数据在同一个事务/查询系统内；
2. Memory corpus 与 filter/search 负载增长到本地 exact scan 或单进程 side index 不再满足 latency；
3. 多进程或远程 writer 成为真实部署要求；
4. JSON metadata 需要大量数据库内索引与复杂查询；
5. PostgreSQL backup、observability 或运维能力开始比 embedded simplicity 更有价值。

从 SQLite 迁移到 PostgreSQL / pgvector 的成本是可控但非零：DDL/type 差异、identity/sequence、timestamp、JSON、boolean、migration transaction 和 backup tooling 都要处理。对单用户规模，数据搬迁本身通常不是主要风险；真正风险是把 product semantics 写成 SQLite rowid、SQLite JSON expression 或 PostgreSQL `jsonb` / vector operator。

### 3.5 Recommendation boundary

**Adopt now**

- P1 默认 SQLite。
- P1 使用关系表表达 durable identity / causal links；metadata 可以保存为应用拥有的 versioned JSON，但不把关键状态只埋在 opaque JSON 中。
- 使用显式 migration 与 SQLite-aware backup/restore 验证。

**Preserve boundary**

- Product/Application 层只认识 Conversation、Message、Turn、Execution identity、status、provenance 与 repository contract。
- 数据库生成方式、JSON query、全文/向量 operator 与 backup implementation 留在 persistence adapter。
- 使用可导出的稳定 ID 与 UTC timestamp；不以 SQLite `rowid` 作为跨系统 product identity。

**Defer**

- PostgreSQL service、pgvector、approximate index、embedding storage 与 vector tuning。
- 不因“未来可能有 Memory”就在 P1 安装 pgvector。

**Migration trigger**

- 任一前述 P2 压力经 prototype/evaluation 证实，或者部署变成多 backend process / remote database 时，重新选择 PostgreSQL。

**结论：** SQLite 是 P1 推荐默认值，confidence **High**。未来迁移风险可接受，前提是从第一天保护 product identity、portable data export 与 persistence boundary。

## 4. Question B — HTTP + SSE vs WebSocket

### 4.1 Observed facts

- Spring MVC 原生支持 `SseEmitter`，也能把 reactive multi-value return 以 `text/event-stream` 输出。MVC 可以承接 reactive backpressure，但最终 response write 仍是 blocking I/O，由配置的 executor 执行。[S13]
- Servlet API 不会在 peer 离开时立即通知应用；Spring 建议流式响应周期性写 heartbeat，以便通过 write failure 发现断连。`SseEmitter` 支持 timeout、completion 与 error lifecycle。[S13]
- WebFlux 是全链路 non-blocking，并原生支持 Reactive Streams backpressure 与 `Flux<ServerSentEvent<?>>`；Spring MVC 与 WebFlux 都能表达 P1 SSE。[S14]
- Spring 官方认为 HTTP streaming / polling 对低消息量动态更新是简单有效的；WebSocket 最适合 low latency、high frequency、high volume 的组合，并额外引入 full-duplex connection lifecycle 与 proxy considerations。[S15]
- HTML Standard 的 `EventSource` 会重连，并携带 `Last-Event-ID`；这是一项 transport replay mechanism，不会自动恢复应用的 durable execution state。[S16]
- AgentScope Java `v2.0.3` 的 `streamEvents` 提供 typed `AgentEvent` reactive stream，interrupt 在 reasoning / acting / streaming checkpoint 协作执行；该事实使 Reactor adaptation 方便，但不要求整个 HTTP stack 都采用 WebFlux。[S17]

### 4.2 Does P1 require a bidirectional persistent transport?

不需要。P1 的 client-to-server 动作是低频 command：发送消息、请求取消、读取 Conversation / Execution。server-to-client 才是持续流：text delta、少量 execution status、terminal event。把 client command 留在普通 HTTP，可以获得天然 request identity、status code、认证/重试语义和更简单的调试路径。

Candidate interaction：

```text
HTTP command: create Turn / Execution
        ↓ returns stable IDs
SSE: observe execution events
        ↓
HTTP query: fetch durable Conversation / final Execution state

HTTP command: request cancellation by execution identity
```

这里不决定具体 URL。关键语义是：

```text
SSE connection closed ≠ execution cancelled
```

浏览器 refresh、Wi-Fi 切换、proxy timeout 或 client 主动关闭 EventSource 都只代表 observer 离开。取消必须是带 `executionId` 的显式 command；backend 持久化 cancel outcome/intention，再请求 runtime 停止。

### 4.3 Spring implementation choice

P1 数据访问是 blocking JDBC，用户规模为一，Spring MVC 的 operational model 更一致。`SseEmitter` 或 MVC 对 `Flux` 的 streaming adaptation 都足够；AgentScope `Flux<AgentEvent>` 可以在 adapter 边界订阅/映射。此处推荐 **Spring MVC + SSE** 作为最小默认，但不把 `SseEmitter` 类型穿透 Application 层。

选择 WebFlux 的合理条件应来自全链路 non-blocking I/O、连接规模或团队已接受的 reactive error/cancellation discipline，而不是“AgentScope 返回 Flux”。如果未来选 WebFlux，blocking JDBC 必须明确调度隔离，复杂度需要有真实收益。

### 4.4 Event and reconnection semantics

Transport-independent event 至少区分：

- execution accepted / started；
- assistant text delta（ephemeral presentation，可丢）；
- execution completed / failed / cancelled（对应 durable terminal fact）；
- optional heartbeat（transport health，无产品语义）。

P1 不需要 durable replay 每个 token delta，也不需要承诺 `Last-Event-ID` 回放。刷新或失联后的最小恢复是：重新读取 durable Conversation 与 Execution final state；若 execution 仍在当前进程运行，可重新 attach live stream，但不是 P1 durability 的事实来源。只有未来确实要求无缝恢复细粒度 progress，才引入 bounded event replay。

Completion path 应先以 product transaction 提交 assistant Message 与 Execution terminal state，再发 terminal notification。若 terminal SSE 丢失，client 仍能通过 query 得到事实；不能让“客户端看到了 completed event”成为数据库 completion 的依据。

### 4.5 Future compatibility

- Background completion 可以复用同一 transport-independent event vocabulary，再由独立 delivery policy 决定是否推送；不需要因此改为 WebSocket。
- 多 client presence 若只需低频同步，可以先 SSE + query；需要双向 presence、typing 或高频协作时再评估 WebSocket。
- Realtime Voice 是低延迟双向 media/control 问题，可能使用 WebSocket、WebRTC 或供应商 realtime protocol；P1 SSE 不声称解决它。
- Device capability negotiation 也应通过独立 protocol boundary 演进，不把 P1 SSE event 当作永久 wire schema。

### 4.6 Recommendation boundary

**P1 should support**

- 普通 HTTP create/query command；
- server-to-client SSE text/status stream；
- 显式 cancellation command；
- refresh/reconnect 后读取 durable Conversation 和 Execution state；
- timeout/error/completion handling 与必要 heartbeat。

**P1 should explicitly not support**

- WebSocket/STOMP infrastructure；
- token delta durable replay；
- disconnect-implies-cancel；
- realtime voice、presence protocol 或跨设备 capability channel；
- 用 SSE connection lifetime 充当 Execution lifetime。

**结论：** HTTP + SSE 是 P1 推荐，confidence **High**。Spring MVC / WebFlux 的最终实现选择可以在首版 Architecture Authority 中收敛，但不影响 transport semantics；当前最小默认是 Spring MVC SSE。

## 5. Question C — Minimum durable P1 state

### 5.1 Why all four identities exist

| Concept | Why it exists | Durable in P1? | Minimum information surviving restart | Do not store yet |
| :-- | :-- | :-- | :-- | :-- |
| Conversation | 唯一长期产品容器与 Timeline ownership boundary | **Yes** | stable ID、创建/更新时间、必要 version/order cursor | AgentScope state blob、prompt、Memory index |
| Message | 用户真正说过或 Alice 已正式提交到 Timeline 的内容 | **Yes** | stable ID、conversation/turn link、author/role、committed content、stable order/time、必要 provenance | 未完成 token delta、chain-of-thought、raw runtime event |
| Turn | 一次用户 interaction 的产品 identity；把 user input、outcome 与可能多次 execution 分开 | **Yes** | stable ID、conversation link、initiating user-message link、created time、causal/idempotency identity | workflow graph、retry policy、background task lifecycle |
| Execution | 某次 runtime attempt；解释 running/crash/cancel/failure，不能冒充 Turn | **Yes** | stable ID、turn link、status、started/finished time、runtime correlation、最小 failure/cancel fact | 完整 stack trace、每个 token/tool event、AgentState 副本、distributed lease |

P1 仍需要 Turn 与 Execution 分离。即使 happy path 是一 Turn 一 Execution，crash 后重试、显式 cancel、未来 provider retry 或 duplicate HTTP command 都不能创建第二个用户 interaction。Turn 是 product causality；Execution 是可失败、可替换的 mechanism。

### 5.2 Smallest useful status model

建议 P1 Execution 采用五个持久状态：

```text
RUNNING
COMPLETED
FAILED
CANCELLED
INTERRUPTED
```

- `RUNNING` 表示 OpenAlice 已持久化并尝试交给 runtime；它不是“线程一定还活着”的证明。
- `COMPLETED` 只在最终 assistant outcome 已由 Conversation Application 正式提交时成立。
- `FAILED` 表示进程仍在、已观察到明确失败并完成终态提交。
- `CANCELLED` 表示用户取消已成为产品终态；runtime 晚到 output 不能再提交为正常 Alice Message。
- `INTERRUPTED` 用于 startup reconciliation：上次进程留下的 `RUNNING` 没有完成事实，当前进程不能证明它成功。

`PENDING` 对 P1 不是必要状态。创建 Turn、User Message 与 `RUNNING` Execution 可以在一个短 transaction 中完成；若 runtime 尚未真正开始就 crash，重启后把该 Execution 解释为 `INTERRUPTED` 仍然诚实。未来若引入 durable queue/admission，再增加 `PENDING`。

取消请求与最终 `CANCELLED` acknowledgement 之间可能有短暂窗口。P1 可记录最小 cancel-request fact/time，随后终结为 `CANCELLED`；不需要为它建立完整子状态机。若 blocking Tool 无法及时停止，如何处理副作用仍是 prototype / reconciliation 问题。

### 5.3 Mandatory crash scenario

```text
1. transaction commits Conversation + Turn + User Message + RUNNING Execution
2. Agent execution begins
3. process crashes before final assistant outcome commits
4. application restarts
5. startup reconciliation changes stale RUNNING → INTERRUPTED
6. Timeline still contains User Message; no completed Assistant Message exists
```

这足以解释“用户说了什么、哪次 Turn 没有完成、为什么没有 Alice 回复”，并防止：

- 丢失 User Message；
- 把 partial stream 拼成 fabricated completed answer；
- refresh/retry 创建重复 Turn；
- 用 AgentScope session 的存在推断 product completion。

Assistant output 推荐只在完整 outcome 被接受时持久化为正常 Message。P1 可以完全不保存 partial output；若为了 UI 明确展示 incomplete content，必须有显式 incomplete semantics，不能复用 committed assistant Message。当前最小方案选择“不保存 partial token”。

### 5.4 Cancellation scenario

```text
User Message durable
Execution RUNNING
user sends explicit cancel(executionId)
cancel intent/outcome becomes durable
runtime interrupt is requested
late completion is rejected by terminal-state check
```

取消 HTTP response 只表示 command 被接受或已终结，不代表底层 Tool 已撤销外部副作用。P1 尚无 Action/Tool side-effect contract，因此不建立 exactly-once 或 compensation；只保证 Alice Timeline 不把已取消 Execution 的迟到输出作为正常完成回复。

### 5.5 Commit boundaries and idempotency

建议两个最小 transaction boundary：

1. **Admission transaction**：用 client request / idempotency identity 创建或复用 Turn，保存 User Message，并创建 Execution `RUNNING`。
2. **Outcome transaction**：检查 Execution 仍可完成，插入 committed Assistant Message，并把 Execution 改为 `COMPLETED`；失败、取消或启动恢复各自只提交对应终态。

这不是 Workflow Engine，也不承诺 exactly-once。它只用 unique identity / terminal transition 防止同一请求或迟到 result 重复写 Timeline。P1 不需要 durable event log、outbox、attempt history 或 distributed lease。

### 5.6 AgentScope mapping

AgentScope Java `v2.0.3` 的事实边界：[S17][S18][S19]

- `(userId, sessionId)` 选择 `AgentState` slot，同 session call 自动串行；这可以映射 runtime lane，但不能定义 OpenAlice Conversation / Turn。
- `AgentState` 保存 model-visible context、summary、permission/tool state，并主要在 call exit 写入；call 中间变化主要在内存中。
- `RuntimeContext` 是 per-call metadata，不是 persistent state。
- `InterruptControl` 是 transient、不会写入 state store；graceful shutdown marker 与 user cancel 也不是同一个 product fact。
- release test 验证 model stream 失败时保留 user input、不保存 incomplete model/tool output；这与 OpenAlice crash policy 方向一致，但只证明 AgentScope 自己的 context behavior。

推荐映射：

| OpenAlice | AgentScope use | Boundary |
| :-- | :-- | :-- |
| Conversation ID | 可用于选择稳定 session slot | AgentScope session 不是 Conversation source of truth |
| Execution ID | 作为 per-call correlation metadata；具体 run-handle API 待 spike | OpenAlice 先持久化并拥有 identity |
| User Message | 作为当前 call input | AgentScope context copy 不替代 durable Message |
| Execution status | 由 adapter 根据 stream/call/interrupt 结果映射 | AgentScope completion event 不能直接写 Timeline |
| Assistant outcome | adapter 返回 candidate result | Conversation Application transaction 决定是否提交 |

### 5.7 Recommendation boundary

**Adopt now**

- 四个 durable identity：Conversation、Message、Turn、Execution。
- 五状态 Execution model；startup 把遗留 `RUNNING` 对账为 `INTERRUPTED`。
- User Message 在 runtime call 前提交；normal Assistant Message 只在完整 outcome transaction 中提交。
- 显式 cancellation 按 Execution identity；terminal transition 拒绝迟到结果。

**Preserve boundary now**

- Turn 不等于 Execution；Conversation 不等于 AgentScope session；Message 不等于 runtime event。
- SSE delta 是 presentation，不是 durable Message。
- 具体 table、column、ID format 与 ORM mapping 留到 implementation design。

**Needs Prototype**

- AgentScope `v2.0.3` bare ReAct / selective Harness 在 slow Tool cancel、hard process termination、shutdown state save 与 restart 对账时的实际行为。
- MVC SSE disconnect / timeout 与 AgentScope subscription disposal 的连接；验证断连默认不取消 product Execution。

**Deferred**

- PENDING queue、retry attempt history、partial token persistence、tool event ledger、outbox、distributed lease、background Result / Delivery state。

## 6. Impact on Architecture Synthesis

| Question | Current draft position | New evidence | Recommendation | Accepted decision changed? |
| :-- | :-- | :-- | :-- | :-- |
| Database | 存储与 Schema OPEN；P1 durable Conversation ACCEPTED | SQLite 同机低并发适配明确；JDBC 可用但 ORM 是 community tier；PostgreSQL/pgvector 能力更强且运维更重 | P1 SQLite；保护 migration boundary；P2 触发条件出现再迁移 | **No**，关闭一个 OPEN candidate |
| Transport | HTTP + SSE 是较稳定候选；WebSocket 延后 | Spring MVC/WebFlux 均支持 SSE；Spring 只在低延迟+高频+高消息量组合下强推 WebSocket | P1 HTTP + SSE + explicit HTTP cancel；re-fetch durable state | **No**，收敛 candidate |
| Durable state | 四 identity / provenance 候选；状态机 OPEN | crash/cancel 场景要求 Turn 与 Execution 分离；AgentScope interrupt/runtime context 不持久 | 四 identity；五个 Execution status；不保存 partial token | **No**，关闭 P1 execution-state OPEN |
| AgentScope | OpenAlice owns product semantics；Selective Harness NEEDS PROTOTYPE | v2.0.3 session/state/cancel lifecycle 与产品状态仍不等价 | 仅做 correlation / runtime mapping | **No**，强化既有 accepted boundary |

没有新证据推翻 accepted assumption。Architecture Synthesis Draft 不需要在本任务中重写；User + Web 接受本报告后，再把候选写入首版 Architecture Authority。

## 7. Final recommendation

### Adopt if User + Web accepts

- P1 database：SQLite，固定已修复 WAL 问题的版本，local filesystem、短 transaction、显式 migration、backup + restore test。
- P1 transport：ordinary HTTP command/query + downstream SSE，cancel 为独立 command。
- P1 durable state：Conversation / Message / Turn / Execution 四 identity；Execution 使用 `RUNNING / COMPLETED / FAILED / CANCELLED / INTERRUPTED`。
- Admission 与 outcome 分成两个 product transaction boundary；Assistant Message 只在完整 outcome 被接受时提交。

### Preserve boundary now

- Database vendor、ORM、JSON/vector operator 不进入 product semantics。
- Event vocabulary 与 execution lifecycle 独立于 SSE / WebSocket。
- SSE disconnect 不等于 cancel；AgentScope session / AgentState 不等于 OpenAlice durable Conversation。
- Stable ID、causal link、provenance、UTC time 与 terminal transition 必须可迁移。

### Needs Prototype

- AgentScope slow Tool cancellation、hard crash / graceful shutdown、late result 与 OpenAlice terminal transition 对账。
- MVC SSE adapter 在 timeout/disconnect 时的 subscription/resource behavior；该小实验验证 adapter，不重新选择产品 transport。

### Deferred

- PostgreSQL / pgvector、embedding/index tuning、vector migration implementation。
- WebSocket、STOMP、Last-Event-ID durable replay、token replay、Voice/realtime protocol。
- PENDING queue、retry attempts、partial output recovery、outbox、distributed coordination。

### Remaining OPEN

- Spring MVC `SseEmitter` 与 MVC reactive return 两种具体 adapter 写法，哪一种在 AgentScope spike 中更清晰。
- SQLite 最终 JDBC / migration dependency 版本与具体 PRAGMA/pool values；这些不改变数据库选择。
- P2 首个 semantic Memory workload 是否达到 PostgreSQL/pgvector migration trigger。

以上 OPEN 不阻止 User + Web 建立第一版 P1 Architecture Authority。

## 8. Sources

| ID | Source |
| :-- | :-- |
| S1 | SQLite official：[Appropriate Uses For SQLite](https://www.sqlite.org/whentouse.html)，页面更新 2025-05-31 |
| S2 | SQLite official：[Write-Ahead Logging](https://www.sqlite.org/wal.html)，包含 concurrency、durability、WAL files 与 2026 WAL-reset fix |
| S3 | SQLite official：[Online Backup API](https://www.sqlite.org/backup.html)、[`VACUUM INTO`](https://www.sqlite.org/lang_vacuum.html#vacuuminto) |
| S4 | SQLite official：[JSON Functions And Operators](https://www.sqlite.org/json1.html) |
| S5 | Xerial SQLite JDBC：[`3.53.4.0` release](https://github.com/xerial/sqlite-jdbc/releases/tag/3.53.4.0)、[repository usage](https://github.com/xerial/sqlite-jdbc/tree/3.53.4.0)，release commit `cab7981` |
| S6 | PostgreSQL 18 official：[MVCC Introduction](https://www.postgresql.org/docs/18/mvcc-intro.html) |
| S7 | PostgreSQL 18 official：[JSON Types](https://www.postgresql.org/docs/18/datatype-json.html)、[`pg_dump`](https://www.postgresql.org/docs/18/backup-dump.html) |
| S8 | pgvector official：[`v0.8.6` README](https://github.com/pgvector/pgvector/blob/v0.8.6/README.md) |
| S9 | Spring Boot official：[SQL Databases](https://docs.spring.io/spring-boot/reference/data/sql.html) |
| S10 | Hibernate ORM `7.1.36.Final` official：[Dialects](https://docs.hibernate.org/orm/7.1/dialect/) |
| S11 | Redgate Flyway official：[SQLite driver reference](https://documentation.red-gate.com/flyway/reference/database-driver-reference/sqlite) |
| S12 | Redgate Flyway official：[PostgreSQL driver reference](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database) |
| S13 | Spring Framework official：[Spring MVC Asynchronous Requests / SSE](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html) |
| S14 | Spring Framework official：[Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)、[controller return values](https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-methods/return-types.html) |
| S15 | Spring Framework official：[When to use WebSocket](https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html) |
| S16 | WHATWG HTML Standard：[Server-sent events](https://html.spec.whatwg.org/multipage/server-sent-events.html) |
| S17 | AgentScope Java official `v2.0.3`：[Agent / execution and streaming](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/docs/v2/en/docs/building-blocks/agent.md)、[Context & AgentState](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/docs/v2/en/docs/building-blocks/context.md) |
| S18 | AgentScope Java official `v2.0.3`：[release notes](https://github.com/agentscope-ai/agentscope-java/releases/tag/v2.0.3)，release commit `1b8e3dc` |
| S19 | AgentScope Java official `v2.0.3`：[`ReActAgentCallFailurePersistenceTest`](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.3/agentscope-core/src/test/java/io/agentscope/core/agent/ReActAgentCallFailurePersistenceTest.java) |
