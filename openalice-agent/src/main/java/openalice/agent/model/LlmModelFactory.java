package openalice.agent.model;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import openalice.agent.config.AgentRuntimeProperties;
import openalice.agent.config.LlmProvider;

/**
 * Builds the {@link Model} backing the agent runtime from configuration.
 *
 * <p>Provider selection: an explicit {@code OPENALICE_LLM_PROVIDER} wins; otherwise the
 * default ({@code auto}) prefers AgentRouter, then DeepSeek, then the deterministic mock.
 * API keys are read from environment variables at build time and are never stored.</p>
 */
public final class LlmModelFactory {

    private LlmModelFactory() {
    }

    public static Model create(AgentRuntimeProperties properties) {
        LlmProvider provider = resolve(properties.llmProvider());
        return switch (provider) {
            case MOCK -> new DeterministicChatModel(properties.replyPrefix());
            case DEEPSEEK, AGENTROUTER -> openAiChatModel(provider, properties);
            case AUTO -> throw new IllegalStateException("LLM provider resolved to AUTO; this is a bug");
        };
    }

    private static LlmProvider resolve(String configured) {
        LlmProvider requested = LlmProvider.parse(configured);
        if (requested != LlmProvider.AUTO) {
            return requested;
        }
        if (hasEnv(AgentRuntimeProperties.ENV_AGENTROUTER_API_KEY)) {
            return LlmProvider.AGENTROUTER;
        }
        if (hasEnv(AgentRuntimeProperties.ENV_DEEPSEEK_API_KEY)) {
            return LlmProvider.DEEPSEEK;
        }
        return LlmProvider.MOCK;
    }

    private static boolean hasEnv(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private static Model openAiChatModel(LlmProvider provider, AgentRuntimeProperties properties) {
        String apiKey = System.getenv(provider.apiKeyEnv());
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "LLM provider '" + provider.configValue() + "' selected but environment variable "
                            + provider.apiKeyEnv() + " is not set"
            );
        }
        String baseUrl = firstNonBlank(properties.llmBaseUrl(), provider.defaultBaseUrl());
        String modelName = firstNonBlank(properties.llmModel(), provider.defaultModel());
        boolean codexClientFingerprint = provider == LlmProvider.AGENTROUTER;

        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .stream(true)
                .httpTransport(ConfiguredHttpTransport.create(properties.llmProxy(), codexClientFingerprint))
                .build();
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
