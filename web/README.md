# OpenAlice Web

`web/` 是未来前端工程占位，不进入 Maven Reactor。

## 约定

- 当前不实现任何前端代码；
- 未来技术栈尚未确认；
- 只通过 `openalice-server` 暴露的 HTTP / SSE / WebSocket API 通信；
- 后端构建与前端构建互不干扰。

## 后端启动

在仓库根目录执行：

```bash
mvn -pl openalice-server -am spring-boot:run
```
