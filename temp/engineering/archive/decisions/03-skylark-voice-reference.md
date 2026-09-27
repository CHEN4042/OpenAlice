# 03 · Skylark 参考评估（Java 语音链路组件地图）

| 元信息 | 值 |
| :--- | :--- |
| 日期 | 2026-09-04 |
| 性质 | **参考评估（非选型决策）** —— 外部开源工程实证；定位为语音链路（VAD/ASR/TTS/RTC）**组件地图与编排思路参考**，供《需求说明书》§15 语音选型调研时实证 |
| 状态 | ✅ 已评估（待 §15 调研展开 / 语音选型定稿后回写结论） |
| 关联 | [02 · MelonPaw 参考评估](./02-melonpaw-reference.md)（AS 2.0 工程范式，另一参考）；需求说明书 §15（ASR/TTS/VAD/云服务商待定事项） |
| 参考对象 | [Jashinck/Skylark](https://github.com/Jashinck/Skylark)（云雀，Apache-2.0）—— Java 全栈智能语音交互系统 |

---

## 1. 评估背景

- 本项目形态：语音为主（首版**半双工**点按说话 + 文本兜底），Agent 底座已定 AgentScope Java 2.0（见 01）。
- 语音技术栈（VAD / ASR / TTS / RTC 供应商）在需求书 §15 仍为**待定事项** → 需要「纯 Java 语音链路」的工程实证作选型输入。
- 候选：Skylark —— 目前公开仓库中少见的**纯 Java 语音 Agent 系统**，覆盖 VAD→ASR→Agent→TTS→RTC 全链路，并含**全双工（可打断）**实现。

## 2. 技术栈事实（2026-09-04 源码核实）

| 环节 | 实现 | 备注 |
| :--- | :--- | :--- |
| AgentScope | `io.agentscope:agentscope:1.0.9`（**1.x**） | ❌ 与选定的 2.0 不匹配，agent 集成代码不可照搬 |
| Web 壳 | Spring Boot 3.2.0（starter-web + **websocket** + webflux） | 同代 |
| Java | 17 | OK |
| VAD | **Silero VAD (ONNX)** + 自研 `TripleVADEngine`（三重 VAD 融合） | ✅ VAD 候选实证 |
| ASR | Vosk（本地小模型）/ 流式 ASR / 通义 ASR 适配器 | ✅ ASR 候选实证 |
| TTS | 流式 TTS / 通义 TTS 适配器；**MaryTTS 为占位符（生成静音 WAV）** | ⚠️ 该工程未完成项，勿直接采信 |
| RTC | Kurento（专业媒体服务器）/ LiveKit（云原生）/ Agora（PAAS）三策略可切换 | ✅ WebRTC 三方案对照 |
| 编排 | `OrchestrationService`：VAD→ASR→AgentScope→TTS 流水线 + 会话缓冲 | — |
| 全双工 | `duplex/`：状态机 / 回声消除 / Backchannel 过滤 / Barge-in 打断 | 本项目将来升级参考 |

分层：`common` / `application`（controller · service · **duplex**）/ `infrastructure`（config · adapter · websocket）；语音与 RTC 供应商全部**接口 + 实现**化（Direct / Http / 多供应商 strategy 切换）。

## 3. 可借鉴点（语音链路，非 agent 部分）

1. **语音编排参考**：VAD→ASR→Agent→TTS 流水线组织方式、会话级音频缓冲、半双工与全双工两套编排并存。
2. **全双工状态机蓝本**：`DuplexSessionStateMachine`（会话状态机）、`ServerAECProcessor`（服务端回声消除）、`BackchannelFilter`（“嗯哼/附和”过滤）、Barge-in 打断 —— 本项目首版半双工，**将来升全双工时是现成设计蓝本**。
3. **供应商适配器模式**：VAD / ASR / TTS / WebRTC 各自抽象接口 + Direct/Http/多供应商实现 + 配置切换 → 适合本项目「LLM/ASR/TTS 正式选型未定、先接口化后插实现」的现状。
4. **RTC 三方案对照**：Kurento（自建媒体服务器，可控）/ LiveKit（云原生轻量）/ Agora（商用 PAAS，含 AI 降噪弱网优化）——可作为 RTC 选型的横向实证输入。

## 4. 不可照搬 / 注意点

1. **Agent 集成是 1.x 旧范式**：每会话一个 `ReActAgent` + `InMemoryMemory`（ConcurrentHashMap 持实例），2.0 已被 AgentStateStore / workspace 取代 —— 本项目按 02（MelonPaw 范式）走 2.0。
2. **工程完成度有限**：TTS 为占位实现；README 自述 MaryTTS 有 Maven 依赖坑；定位是「组件地图 + 编排思路」，非可直接抄的成品。
3. 其业务人设（专业培训讲师 / 垂直域）与本项目无关，忽略。

## 5. 结论与后续

- ✅ 结论：作为**语音链路组件与编排的工程实证**记录在案；agent 部分不参考。
- ➡️ 后续：在《需求说明书》§15 语音选型调研时，把 Skylark 的三套 VAD/ASR/TTS/RTC 用法与官方（通义音频 / Qwen 多模态）能力横向对比后再定选型；选型定稿后回写本文件结论。
- 🔗 参考副本：本地 `/tmp/Skylark-research`（调研用，不入库）。
