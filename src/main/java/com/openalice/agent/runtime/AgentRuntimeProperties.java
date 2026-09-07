package com.openalice.agent.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Runtime configuration for the AgentScope-backed agent.
 *
 * <p>LLM fields are intentionally provider-agnostic: they are plain strings that
 * {@code com.openalice.agent.llm.LlmModelFactory} interprets. API keys are never stored
 * here (or in any file) — they are read from environment variables at model build time.</p>
 *
 * @param agentName   display name of the agent
 * @param description short description of the agent
 * @param systemPrompt system prompt
 * @param replyPrefix prefix prepended by the deterministic (mock) model
 * @param timeout     max time to wait for one agent reply
 * @param workspace   AgentScope scratch workspace
 * @param llmProvider provider selection: auto | mock | deepseek | agentrouter
 * @param llmModel    model name override (provider-specific default when null)
 * @param llmBaseUrl  API base URL override (provider-specific default when null)
 * @param llmProxy    optional HTTP proxy as host:port, e.g. 127.0.0.1:7897
 */
public record AgentRuntimeProperties(
        String agentName,
        String description,
        String systemPrompt,
        String replyPrefix,
        Duration timeout,
        Path workspace,
        String llmProvider,
        String llmModel,
        String llmBaseUrl,
        String llmProxy
) {

    public static final String ENV_SYSTEM_PROMPT = "OPENALICE_SYSTEM_PROMPT";
    public static final String ENV_PROVIDER = "OPENALICE_LLM_PROVIDER";
    public static final String ENV_MODEL = "OPENALICE_LLM_MODEL";
    public static final String ENV_BASE_URL = "OPENALICE_LLM_BASE_URL";
    public static final String ENV_PROXY = "OPENALICE_LLM_PROXY";

    public AgentRuntimeProperties {
        agentName = normalize(agentName, "Alice");
        description = normalize(description, "OpenAlice baseline agent");
        systemPrompt = normalize(systemPrompt, "You are Alice, a warm and attentive AI companion.");
        replyPrefix = replyPrefix == null ? "收到：" : replyPrefix;
        timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
        workspace = workspace == null ? defaultWorkspace() : workspace;
        llmProvider = trimToNull(llmProvider);
        llmModel = trimToNull(llmModel);
        llmBaseUrl = trimToNull(llmBaseUrl);
        llmProxy = trimToNull(llmProxy);
    }

    public static AgentRuntimeProperties defaults() {
        return new AgentRuntimeProperties(null, null, null, null, null, null, null, null, null, null);
    }

    /** Builds properties from the {@code OPENALICE_*} environment variables (see constants above). */
    public static AgentRuntimeProperties fromEnvironment() {
        AgentRuntimeProperties base = defaults();
        return new AgentRuntimeProperties(
                base.agentName(),
                base.description(),
                System.getenv(ENV_SYSTEM_PROMPT),
                base.replyPrefix(),
                base.timeout(),
                base.workspace(),
                System.getenv(ENV_PROVIDER),
                System.getenv(ENV_MODEL),
                System.getenv(ENV_BASE_URL),
                System.getenv(ENV_PROXY)
        );
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Path defaultWorkspace() {
        Path workspace = Path.of(
                System.getProperty("java.io.tmpdir"),
                "openalice-agentscope-workspace"
        );
        try {
            Files.createDirectories(workspace);
            return workspace;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create AgentScope workspace", exception);
        }
    }
}
