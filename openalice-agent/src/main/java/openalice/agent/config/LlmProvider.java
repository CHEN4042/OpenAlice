package openalice.agent.config;

import java.util.Locale;

/**
 * LLM backend selection for the agent runtime.
 *
 * <p>Provider resolution is intentionally explicit and order-independent:
 * {@code AUTO} (the default when no env var is set) prefers AgentRouter, then
 * DeepSeek, then falls back to the deterministic mock model.</p>
 */
public enum LlmProvider {

    AUTO(null, null, null),
    MOCK(null, null, null),
    DEEPSEEK(
            "https://api.deepseek.com",
            "deepseek-v4-flash",
            AgentRuntimeProperties.ENV_DEEPSEEK_API_KEY
    ),
    AGENTROUTER(
            "https://agentrouter.org",
            "deepseek-v4-flash",
            AgentRuntimeProperties.ENV_AGENTROUTER_API_KEY
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

    /** Parses the {@code OPENALICE_LLM_PROVIDER} value; null/blank means {@code AUTO}. */
    public static LlmProvider parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> AUTO;
            case "mock" -> MOCK;
            case "deepseek" -> DEEPSEEK;
            case "agentrouter", "relay" -> AGENTROUTER;
            default -> throw new IllegalArgumentException(
                    "Unsupported LLM provider '" + value
                            + "' (expected one of: auto, mock, deepseek, agentrouter)"
            );
        };
    }
}
