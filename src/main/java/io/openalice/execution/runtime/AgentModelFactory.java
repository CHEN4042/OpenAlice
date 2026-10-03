package io.openalice.execution.runtime;

@FunctionalInterface
public interface AgentModelFactory {
    AgentModelConnection create();
}
