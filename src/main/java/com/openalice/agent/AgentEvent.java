package com.openalice.agent;

/**
 * 智能体运行时的流式事件。
 *
 * <p>约定：不直接传输原始字符串，而是使用下面的具体事件记录
 * （{@link TextDeltaEvent} / {@link DoneEvent} / {@link ErrorEvent}），
 * 方便上层按类型分发、串行化与测试。</p>
 */
public sealed interface AgentEvent permits TextDeltaEvent, DoneEvent, ErrorEvent {
}
