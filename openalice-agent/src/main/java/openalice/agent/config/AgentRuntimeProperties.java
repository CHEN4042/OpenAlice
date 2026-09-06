package openalice.agent.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public record AgentRuntimeProperties(
        String agentName,
        String description,
        String systemPrompt,
        String replyPrefix,
        Duration timeout,
        Path workspace
) {
    public AgentRuntimeProperties {
        agentName = normalize(agentName, "Alice");
        description = normalize(description, "OpenAlice baseline agent");
        systemPrompt = normalize(systemPrompt, "You are Alice, a warm and attentive AI companion.");
        replyPrefix = replyPrefix == null ? "收到：" : replyPrefix;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        workspace = workspace == null ? defaultWorkspace() : workspace;
    }

    public static AgentRuntimeProperties defaults() {
        return new AgentRuntimeProperties(null, null, null, null, null, null);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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
