package com.openalice.llm;

import java.util.Locale;

/**
 * 大模型（LLM）后端选择（LLM 接入层）。
 *
 * <p>provider 解析刻意保持显式、与顺序无关：{@code AUTO}（未配置时的默认值）会先找
 * 已配好 api-key 的供应商（AgentRouter 中转优先、DeepSeek 兜底）。已经连接真实
 * API key，因此不再提供 mock 模型。</p>
 */
public enum LlmProvider {

    AUTO(null, null, null),
    DEEPSEEK(
            "https://api.deepseek.com",
            "deepseek-v4-flash",
            "OPENALICE_DEEPSEEK_API_KEY"
    ),
    AGENTROUTER(
            "https://agentrouter.org",
            "deepseek-v4-flash",
            "OPENALICE_AGENTROUTER_API_KEY"
    );

    private final String defaultBaseUrl;
    private final String defaultModel;
    private final String apiKeyEnv;

    LlmProvider(String defaultBaseUrl, String defaultModel, String apiKeyEnv) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.apiKeyEnv = apiKeyEnv;
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public String apiKeyEnv() {
        return apiKeyEnv;
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 解析 {@code openalice.llm.provider} 的值；null/blank 表示 {@code AUTO}。 */
    public static LlmProvider parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> AUTO;
            case "deepseek" -> DEEPSEEK;
            case "agentrouter", "relay" -> AGENTROUTER;
            default -> throw new IllegalArgumentException(
                    "Unsupported LLM provider '" + value
                            + "' (expected one of: auto, deepseek, agentrouter)"
            );
        };
    }
}
