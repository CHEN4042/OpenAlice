package com.openalice.agent.tool;

import io.agentscope.core.tool.Tool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例工具：返回当前系统时间。
 *
 * <p>用途是打通 ReAct“推理 → 调用工具 → 观察结果”的链路：当用户询问“现在几点 /
 * 今天几号”等需要绝对时间的问题时，模型会调用 {@code get_current_time} 拿到结果再作答。
 * 初期只保留这一个示例工具，后续按需在此扩展。</p>
 */
public final class CurrentTimeTool {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(
            name = "get_current_time",
            description = "获取当前系统时间。当用户询问当前时间、日期等需要绝对时间的问题时使用。",
            readOnly = true
    )
    public String getCurrentTime() {
        return "当前系统时间：" + LocalDateTime.now().format(FORMATTER);
    }
}
