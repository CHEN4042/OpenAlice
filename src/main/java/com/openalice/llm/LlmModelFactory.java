package com.openalice.llm;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;

/**
 * 根据 {@link LlmSettings} 构建 {@link Model}（LLM 接入的工厂入口）。
 *
 * <p>provider 选择：显式配置 {@code openalice.llm.provider} 优先；默认 {@code auto}
 * 会先找 AgentRouter（中转）的 api-key，再找 DeepSeek 的 api-key，都没有则直接报错
 * （已移除 mock 回退）。api-key 取自 {@link LlmSettings#apiKey()}（由 gitignored 的
 * {@code application-local.yml} 绑定）或供应商对应的环境变量，绝不写进提交的配置。</p>
 */
public final class LlmModelFactory {

    private LlmModelFactory() {
    }

    public static Model create(LlmSettings settings) {
        LlmProvider provider = resolve(settings);
        return switch (provider) {
            case DEEPSEEK, AGENTROUTER -> openAiChatModel(provider, settings);
            case AUTO -> throw new IllegalStateException("LLM provider resolved to AUTO; this is a bug");
        };
    }

    private static LlmProvider resolve(LlmSettings settings) {
        LlmProvider requested = LlmProvider.parse(settings.provider());
        if (requested != LlmProvider.AUTO) {
            return requested;
        }
        if (hasApiKey(settings, LlmProvider.AGENTROUTER)) {
            return LlmProvider.AGENTROUTER;
        }
        if (hasApiKey(settings, LlmProvider.DEEPSEEK)) {
            return LlmProvider.DEEPSEEK;
        }
        throw new IllegalStateException(
                "No LLM API key configured: set openalice.llm.api-key in application-local.yml "
                        + "or export OPENALICE_AGENTROUTER_API_KEY / OPENALICE_DEEPSEEK_API_KEY"
        );
    }

    private static boolean hasApiKey(LlmSettings settings, LlmProvider provider) {
        return apiKey(settings, provider) != null;
    }

    private static String apiKey(LlmSettings settings, LlmProvider provider) {
        return firstNonBlank(settings.apiKey(), System.getenv(provider.apiKeyEnv()));
    }

    private static Model openAiChatModel(LlmProvider provider, LlmSettings settings) {
        String apiKey = apiKey(settings, provider);
        if (apiKey == null) {
            throw new IllegalStateException(
                    "LLM provider '" + provider.configValue() + "' selected but no API key configured: "
                            + "set openalice.llm.api-key in application-local.yml "
                            + "or the environment variable " + provider.apiKeyEnv()
            );
        }
        String baseUrl = firstNonBlank(settings.baseUrl(), provider.defaultBaseUrl());
        String modelName = firstNonBlank(settings.model(), provider.defaultModel());
        boolean codexClientFingerprint = provider == LlmProvider.AGENTROUTER;

        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .stream(true)
                .httpTransport(ConfiguredHttpTransport.create(settings.proxy(), codexClientFingerprint))
                .build();
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
