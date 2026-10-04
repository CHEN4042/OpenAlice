package io.openalice.runtime;

@FunctionalInterface
public interface AgentModelFactory {
    AgentModelConnection create();
}
