package com.openalice.agent;

/** 文本增量事件：一次模型流式输出的一个文本片段。 */
public record TextDeltaEvent(String delta) implements AgentEvent {
    public TextDeltaEvent {
        if (delta == null || delta.isEmpty()) {
            throw new IllegalArgumentException("delta must not be empty");
        }
    }
}
