package io.openalice.execution.runtime;

import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.JdkHttpTransport;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.openalice.config.OpenAliceProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiAgentModelFactory implements AgentModelFactory {

    private final OpenAliceProperties properties;

    public OpenAiAgentModelFactory(OpenAliceProperties properties) {
        this.properties = properties;
    }

    @Override
    public AgentModelConnection create() {
        OpenAliceProperties.Model model = properties.getModel();
        if (!StringUtils.hasText(model.getApiKey())) {
            throw new IllegalStateException(
                    "OPENALICE_MODEL_API_KEY is required only when a real model execution starts");
        }
        if (!StringUtils.hasText(model.getBaseUrl()) || !StringUtils.hasText(model.getModelName())) {
            throw new IllegalStateException("Model base URL and model name must be configured");
        }

        HttpTransportConfig transportConfig = HttpTransportConfig.builder()
                .connectTimeout(model.getConnectTimeout())
                .readTimeout(model.getReadTimeout())
                .responseTimeout(model.getReadTimeout())
                .streamIdleTimeout(model.getReadTimeout())
                .build();
        HttpTransport transport = new ConfiguredHttpTransport(
                JdkHttpTransport.builder().config(transportConfig).build(), model.getHeaders());
        OpenAIChatModel openAiModel = OpenAIChatModel.builder()
                .apiKey(model.getApiKey())
                .baseUrl(model.getBaseUrl())
                .modelName(model.getModelName())
                .stream(true)
                .httpTransport(transport)
                .build();
        return new AgentModelConnection(model.getAlias(), openAiModel, transport::close);
    }
}
