package io.openalice.execution;

import java.time.Instant;
import java.util.UUID;

public record Execution(UUID id, ExecutionStatus status, Instant createdAt, Instant updatedAt) {}
