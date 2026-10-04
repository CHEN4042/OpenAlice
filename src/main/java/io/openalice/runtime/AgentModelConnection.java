package io.openalice.runtime;

import io.agentscope.core.model.Model;

public record AgentModelConnection(String alias, Model model, AutoCloseable resource)
        implements AutoCloseable {

    public AgentModelConnection(Model model) {
        this("test", model, () -> {});
    }

    @Override
    public void close() throws Exception {
        resource.close();
    }
}
