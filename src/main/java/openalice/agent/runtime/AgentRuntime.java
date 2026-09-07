package openalice.agent.runtime;

import openalice.model.ChatMessage;

public interface AgentRuntime extends AutoCloseable {
    ChatResult chat(ChatMessage userMessage);

    @Override
    default void close() {
    }
}

