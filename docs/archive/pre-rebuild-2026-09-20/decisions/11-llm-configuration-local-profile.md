# ADR 11 · LLM 配置落 application.yml + gitignored local profile

- 状态：✅ 当前有效
- 日期：2026-09-08
- 关系：修订 ADR 10 落地时随带的「LLM 配置 = 代码默认值 + env」实现细节；不改变 ADR 07 的双 provider / auto 回退链语义

## 1. 背景

P1.5 / ADR 10 落地后，LLM 配置分散在两处：非敏感默认值（systemPrompt / provider / model / baseUrl / proxy / timeout 等）硬编码在 `AgentRuntimeProperties` record 的构造器里；可覆盖项与 key 走 `OPENALICE_*` 环境变量。用户提出两个问题：

1. **配置放代码不合理**：改配置要重新编译，不可见、不符合 Spring 惯例；
2. 用户偏好 Jarvis 式的「配置写在 application 文件里、打开就能改」。

但仓库按 public 红线维护（Jarvis 曾把真实 key 写进 `application.properties` 并被构建产物复制），**key 不能进提交的文件**。

## 2. 决策

### 2.1 默认值全部移入 application.yml（提交）

`src/main/resources/application.yml` 提供全部非敏感默认值，结构两段：

```yaml
openalice:
  agent:                 # 行为：name / description / system-prompt / reply-prefix / timeout / workspace / context-window-size
  llm:                   # 模型接线：provider / model / base-url / proxy（api-key 不写）
```

### 2.2 本机真实配置走 gitignored 的 application-local.yml

- 新建 `application-local.yml`（**已加入 .gitignore**），放本机私有覆盖，允许写真实 key；
- 启动时 `--spring.profiles.active=local` 激活（IDEA：Run Configuration → Active profiles 填 `local`）；
- 命名取 `local` 而非 `dev`：`dev` 常被当作可入库的环境配置，`local` 在社区惯例中指向「本机私有、别提交」的文件，降低手滑提交风险。

### 2.3 代码只留纯配置容器

- `AgentRuntimeProperties`（agent.runtime）收敛为**无默认值、无 Spring 注解**的 record（增加 `llmApiKey` 字段）；删除 `defaults()` / `fromEnvironment()` / 代码内 normalize 默认值与建目录副作用；
- Spring 绑定收敛在 config 层：新增 `config.OpenAliceSettings`（`@ConfigurationProperties(prefix = "openalice")`，嵌套 Agent / Llm record），由 `OpenAliceConfiguration` 组装为 agent 域 record——保持「agent 内核不依赖 Spring / 配置文件」的包边界；
- `context-window-size` 仍由 `ContextAssembler` 经 `@Value` 读取，键不变。

### 2.4 key 解析顺序与 provider 语义

- key 来源：`openalice.llm.api-key`（仅 local / env）优先，`OPENALICE_DEEPSEEK_API_KEY` / `OPENALICE_AGENTROUTER_API_KEY` 环境变量兜底；
- `provider` 显式（mock / deepseek / agentrouter）时 key 直接归属该 provider；
- `provider: auto` 时回退链不变：中转（agentrouter）→ DeepSeek → mock；若 local 填了 `api-key` 而 provider 为 auto，该 key 按中转（agentrouter）处理——文档与 local 模板均要求「填了 api-key 请同时显式指定 provider」。

## 3. 权衡

| 方案 | 可见性 | key 安全 | 本机体验 |
| :-- | :-- | :-- | :-- |
| 代码默认 + env（原状） | ❌ 不可见、需编译 | ✅ | ❌ 要 export 一堆 env |
| 全量 application.yml + key 走 env | ✅ | ✅ | ⚠️ key 与配置分离，改模型动两处 |
| application.yml + gitignored local（采用） | ✅ | ✅（key 不进提交） | ✅ 一个文件全搞定，接近 Jarvis |

- env 仍可用作覆盖（Spring relaxed binding 会把 `OPENALICE_LLM_PROVIDER` 等映射到 `openalice.llm.*`），但不作为主路径。

## 4. 影响

- README「LLM 接入」由环境变量改为配置文件说明 + IDEA Active profiles 教程；
- `AgentRuntimeProperties` 不再是默认值出处；测试改为显式构造（`AgentRuntimePropertiesFixture`，固定 mock provider 保证确定性）；
- 后续新增 provider / 模型参数：在 `OpenAliceSettings.Llm` 与 application.yml 同步加键即可，agent 内核无需改动。
