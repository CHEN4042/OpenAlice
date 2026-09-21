# A.L.I.C.E.

OpenAlice 是面向唯一用户本人的长期陪伴队友。她在唯一一条主对话里，结合当前消息与记忆边界召回的真实历史接住生活分享；需要事实信息时会自动发起真实联网搜索。首版内置天童爱丽丝人格卡；未来方向是可按需加载、独立替换的人格卡片。

**当前状态：产品契约已冻结，项目进入彻底重构前的 architecture 阶段。** `src/` 中的实现是 legacy reference，不是当前架构真相。

- 产品契约：[SPEC.md](_bmad-output/specs/spec-openalice/SPEC.md)
- 人格卡契约：[persona-contract.md](_bmad-output/specs/spec-openalice/persona-contract.md)
- 回复契约：[conversation-policy.md](_bmad-output/specs/spec-openalice/conversation-policy.md)
- 记忆契约：[memory-contract.md](_bmad-output/specs/spec-openalice/memory-contract.md)
- 当前交接：[handoff.md](handoff.md)
- 历史文档：`docs/archive/pre-rebuild-2026-09-20/`（只作追溯）

## 构建

```bash
mvn clean test
mvn spring-boot:run
```

真实模型 key 只放在 gitignored 的 `application-local.yml` 或环境变量中，禁止提交。
