package com.openalice.agent.runtime;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Immutable runtime configuration for the AgentScope-backed agent.
 *
 * <p>Values are supplied by Spring at the composition root: they are bound from
 * {@code openalice.*} in {@code application.yml} / {@code application-local.yml} (see
 * {@code com.openalice.config.OpenAliceSettings}) and assembled here by
 * {@code com.openalice.config.OpenAliceConfiguration}. This record deliberately has no
 * defaults and no framework annotations so the agent core stays independent of Spring
 * and of configuration files.</p>
 *
 * <p>LLM fields are provider-agnostic strings interpreted by
 * {@code com.openalice.agent.llm.LlmModelFactory}. {@code llmApiKey} must only be set in
 * the git-ignored {@code application-local.yml} or via environment variables — never in
 * the committed {@code application.yml}.</p>
 *
 * @param agentName    display name of the agent
 * @param description  short description of the agent
 * @param systemPrompt system prompt
 * @param replyPrefix  prefix prepended by the deterministic (mock) model
 * @param timeout      max time to wait for one agent reply
 * @param workspace    AgentScope scratch workspace
 * @param llmProvider  provider selection: auto | mock | deepseek | agentrouter
 * @param llmModel     model name override (provider-specific default when null)
 * @param llmBaseUrl   API base URL override (provider-specific default when null)
 * @param llmProxy     optional HTTP proxy as host:port, e.g. 127.0.0.1:7897
 * @param llmApiKey    API key for the selected provider (local file / env only)
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
        String llmProxy,
        String llmApiKey
) {
}
