package com.openalice.agent.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Test fixture for {@link AgentRuntimeProperties}. The production defaults live in
 * application.yml (bound by Spring), so tests build the value object explicitly and pin
 * the deterministic (mock) provider to keep them free of network and environment.
 */
public final class AgentRuntimePropertiesFixture {

    public static final String SYSTEM_PROMPT = "You are Alice, a warm and attentive AI companion.";

    private static final Path WORKSPACE = tempWorkspace();

    private AgentRuntimePropertiesFixture() {
    }

    public static AgentRuntimeProperties mock() {
        return new AgentRuntimeProperties(
                "Alice",
                "OpenAlice test agent",
                SYSTEM_PROMPT,
                "收到：",
                Duration.ofSeconds(60),
                WORKSPACE,
                "mock",
                null,
                null,
                null,
                null
        );
    }

    private static Path tempWorkspace() {
        try {
            return Files.createTempDirectory("openalice-test-workspace");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create test workspace", exception);
        }
    }
}
