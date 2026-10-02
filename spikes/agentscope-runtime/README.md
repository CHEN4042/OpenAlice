# AgentScope Runtime Spike

This isolated Maven project gathers executable evidence for the runtime boundary defined by the
OpenAlice Architecture Spine. It is not an application skeleton and has no production API or
schema.

## Runtime

- Java 21
- AgentScope Java 2.0.3
- JUnit 5
- Spring MVC only for the embedded SSE lifecycle probe
- SQLite only for the disposable hard-crash probe

## Run

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

The tests use deterministic models and tools. No external model, credential, fixed port, or
repository-local database is required. Raw test output is generated under `target/surefire-reports/`
and is intentionally ignored as build output.

## Experiment map

| Experiment | Test |
| :-- | :-- |
| Context/history ownership | `ContextOwnershipSpikeTest` |
| Cancellation and late results | `CancellationSpikeTest` |
| Hard process crash/restart | `CrashRestartSpikeTest` |
| Specialist/subagent completion | `SubagentSpikeTest` |
| SSE disconnect behavior | `SseDisconnectSpikeTest` |
