package io.openalice.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.config.OpenAliceProperties;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiAgentModelFactoryTest {

    @Test
    void buildsConfiguredOpenAiCompatibleConnectionWithoutCallingTheNetwork() throws Exception {
        OpenAliceProperties properties = new OpenAliceProperties();
        properties.getModel().setAlias("provider-a");
        properties.getModel().setBaseUrl("https://example.invalid/v1");
        properties.getModel().setModelName("test-model");
        properties.getModel().setApiKey("test-only-placeholder");
        properties.getModel().setHeaders(Map.of("X-Route", "local-test"));
        properties.getModel().setConnectTimeout(Duration.ofSeconds(3));
        properties.getModel().setReadTimeout(Duration.ofSeconds(20));

        try (AgentModelConnection connection = new OpenAiAgentModelFactory(properties).create()) {
            assertThat(connection.alias()).isEqualTo("provider-a");
            assertThat(connection.model().getModelName()).isEqualTo("test-model");
        }
    }
}
