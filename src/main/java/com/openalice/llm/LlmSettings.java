package com.openalice.llm;

/**
 * LLM 接入的不可变配置值对象。
 *
 * <p>由组合根从 Spring 的 {@code config.OpenAliceSettings.Llm} 复制而来；刻意不带
 * Spring 注解与默认值，保证 llm 接入层不依赖框架与配置文件。{@code apiKey} 只能来自
 * gitignored 的 {@code application-local.yml} 或环境变量，绝不进提交的配置文件。</p>
 *
 * @param provider provider 选择：auto | deepseek | agentrouter
 * @param model    模型名覆盖（null = provider 默认模型）
 * @param baseUrl  接口 base-url 覆盖（null = provider 默认地址）
 * @param proxy    可选 HTTP 代理 host:port，例如 127.0.0.1:7897
 * @param apiKey   所选 provider 的 api-key（仅 local 文件 / 环境变量）
 */
public record LlmSettings(
        String provider,
        String model,
        String baseUrl,
        String proxy,
        String apiKey
) {
}
