package com.openalice.agent.llm;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import com.openalice.agent.runtime.AgentRuntimeProperties;

/**
 * Builds the {@link Model} backing the agent runtime from configuration.
 *
 * <p>Provider selection: an explicit {@code llm.provider} wins; otherwise the default
 * ({@code auto}) prefers AgentRouter, then DeepSeek, then the deterministic mock.
 * The API key is taken from {@link AgentRuntimeProperties#llmApiKey()} (bound from the
 * git-ignored {@code application-local.yml}) or, as a fallback, from the provider's
 * environment variable. Keys are never written to the committed configuration.</p>
 */
public final class LlmModelFactory {

    private LlmModelFactory() {
    }

    public static Model create(AgentRuntimeProperties properties) {
        LlmProvider provider = resolve(properties);
        return switch (provider) {
            case MOCK -> new DeterministicChatModel(properties.replyPrefix());
            case DEEPSEEK, AGENTROUTER -> openAiChatModel(provider, properties);
            case AUTO -> throw new IllegalStateException("LLM provider resolved to AUTO; this is a bug");
        };
    }

    private static LlmProvider resolve(AgentRuntimeProperties properties) {
        LlmProvider requested = LlmProvider.parse(properties.llmProvider());
        if (requested != LlmProvider.AUTO) {
            return requested;
        }
        if (hasApiKey(properties, LlmProvider.AGENTROUTER)) {
            return LlmProvider.AGENTROUTER;
        }
        if (hasApiKey(properties, LlmProvider.DEEPSEEK)) {
            return LlmProvider.DEEPSEEK;
        }
        return LlmProvider.MOCK;
    }

    private static boolean hasApiKey(AgentRuntimeProperties properties, LlmProvider provider) {
        return apiKey(properties, provider) != null;
    }

    private static String apiKey(AgentRuntimeProperties properties, LlmProvider provider) {
        return firstNonBlank(properties.llmApiKey(), System.getenv(provider.apiKeyEnv()));
    }

    private static Model openAiChatModel(LlmProvider provider, AgentRuntimeProperties properties) {
        String apiKey = apiKey(properties, provider);
        if (apiKey == null) {
            throw new IllegalStateException(
                    "LLM provider '" + provider.configValue() + "' selected but no API key configured: "
                            + "set openalice.llm.api-key in application-local.yml "
                            + "or the environment variable " + provider.apiKeyEnv()
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
