package com.openalice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 从 {@code application.yml} / {@code application-local.yml} 中绑定 {@code openalice.*}
 * 前缀的外部配置（通过 {@code --spring.profiles.active=local} 激活本地配置）。
 *
 * <p>{@link Agent} 下的智能体行为默认值可以安全提交到仓库；{@link Llm#apiKey()} 只能写在
 * gitignored 的 {@code application-local.yml} 或环境变量里，保证公共仓库永不携带真实密钥。</p>
 */
@ConfigurationProperties(prefix = "openalice")
public record OpenAliceSettings(Agent agent, Llm llm) {

    /** {@code openalice.agent.*} —— 智能体行为（默认值可安全提交）。 */
    public record Agent(
            String name,
            String description,
            String systemPrompt,
            Duration timeout
    ) {
    }

    /** {@code openalice.llm.*} —— 模型接线配置；{@code api-key} 永不提交。 */
    public record Llm(
            String provider,
            String model,
            String baseUrl,
            String proxy,
            String apiKey
    ) {
    }
}
