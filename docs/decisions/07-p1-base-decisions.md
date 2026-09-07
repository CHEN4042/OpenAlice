# 07 · P1 底座技术决策（v1.5）

| 项目 | 内容 |
| :-- | :-- |
| 状态 | ✅ 已确认 |
| 日期 | 2026-09-07 |
| 修订 | 修订 ADR 01「M1 Redis」前提表述；修订 ADR 06 §2.11（Java 17 → 21）与 §3（P1 范围）；同步《项目需求说明书》v1.5 /《记忆架构设计 v0.4》 |
| 阶段 | P1（会说真话 · 底座） |

## 1. 决策（用户拍板）

1. **砍 Redis**：基础设施收敛为「唯一外部存储 = PostgreSQL（pgvector）」。
   - M1 会话消息经 `core.MemoryPort` 实时落 PG `session_message`；
   - AgentScope 运行态（AgentState）单实例保持**进程内**（沿用 `InMemoryAgentStateStore`），不做 Redis 化；
   - Redis 仅预留（`openalice:` 前缀），未来需要缓存 / 跨实例恢复时再按需引入。
2. **真实 LLM 双 provider**：**中转站（OpenAI 兼容，优先）+ DeepSeek 官方 API（兜底）**；同协议仅 base_url / key / model 不同，故障自动切换；密钥走 `OPENALICE_*` 环境变量，不落盘、不入 Git；移除 Ollama 本地开发轨。
3. **`/chat` 直接流式（SSE）**：文本对话从 P1 起即流式返回（最小事件集 `text_delta / done / error`）；WebSocket 双向通路留给 P3 语音。
4. **单用户 + persona 初始化**：固定唯一用户，`user_id` 仅作存储预留、不作路由键；首次启动引导配置 `persona/` 文件（user.md / persona-config.yml 等，参考 OpenHanako）。
5. **阶段坐标统一 P1–P4**（P = Phase）：P1 会说真话（底座）→ P2 记得住（记忆）→ P3 听得到（语音）→ P4 看得见 / 摸得到（Web / 工具 / 主动）；废弃「功能优先级 P0/P1/P2」旧标记，功能归属并入阶段坐标，避免与阶段 P 编号混淆。

## 2. 修订的既有结论

- **ADR 01**：其"记忆 M1–M3 自研（Redis/PG/pgvector）"中的 **M1-Redis** 部分被本决议修订——M1 改走 PG `session_message` + 进程内 AgentState；"选 AgentScope Java 2.0"结论本身不受影响。
- **ADR 06**：
  - §2.11「Java 目标版本为 17」→ **21（LTS）**（2026-09-07 已随工程升级）；
  - §3「Phase 1 不接真实 LLM API、不做持久化」已过时：P1（底座）范围扩展为真实双 provider 接入 + `/chat` SSE 流式 + M1 会话消息 PG 持久化 + 单用户 persona/ 初始化；已跑通的 in-memory 骨架（`DeterministicChatModel` / `InMemoryMemoryPort`）降级为过渡实现。

## 3. 参考

- 《项目需求说明书》v1.5：§2.1 / §2.2 / §4.1 / §4.3 / §8 / §12
- 《记忆架构设计 v0.4》：§1 / §2 / §3.1 / §8 / §9
- OpenHanako：单用户 + 人格文件初始化的参考形态（仅参考，不约束实现）
