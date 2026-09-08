package com.openalice.agent.tool;

import io.agentscope.core.tool.Toolkit;

/**
 * 组装 AgentScope 智能体可用的工具集（Toolkit）。
 *
 * <p>当前只注册一个示例工具 {@link CurrentTimeTool}，用于打通 ReAct 工具调用链路；
 * 后续新增工具时统一在这里集中注册，避免散落到各个类里。</p>
 */
public final class AgentToolkit {

    private AgentToolkit() {
    }

    public static Toolkit create() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new CurrentTimeTool());
        return toolkit;
    }
}
