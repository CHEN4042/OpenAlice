package com.openalice.agent;

/** 完成事件：携带一轮对话的最终回复全文。 */
public record DoneEvent(String reply) implements AgentEvent {
    public DoneEvent {
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("reply must not be blank");
        }
        reply = reply.trim();
    }
}
