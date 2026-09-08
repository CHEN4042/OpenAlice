package com.openalice.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration bound from {@code openalice.*} in {@code application.yml} /
 * {@code application-local.yml} (activated via {@code --spring.profiles.active=local}).
 *
 * <p>Agent behaviour defaults are safe to commit; {@link Llm#apiKey()} must only be set
 * in the git-ignored {@code application-local.yml} or via environment variables so the
 * public repository never carries a real key.</p>
 */
@ConfigurationProperties(prefix = "openalice")
public record OpenAliceSettings(Agent agent, Llm llm) {

    /** {@code openalice.agent.*} — agent behaviour (committed defaults). */
    public record Agent(
            String name,
            String description,
            String systemPrompt,
            String replyPrefix,
            Duration timeout,
            Path workspace
    ) {
    }

    /** {@code openalice.llm.*} — model wiring; {@code api-key} is never committed. */
    public record Llm(
            String provider,
            String model,
            String baseUrl,
            String proxy,
            String apiKey
    ) {
    }
}
