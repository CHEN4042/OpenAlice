package io.openalice.runtime;

import java.time.Instant;
import java.util.UUID;

public sealed interface RuntimeEvent {

    UUID executionId();

    Instant occurredAt();

    record Started(UUID executionId, Instant occurredAt) implements RuntimeEvent {}

    record TextDelta(UUID executionId, Instant occurredAt, String delta) implements RuntimeEvent {}

    record CandidateResult(UUID executionId, Instant occurredAt, RuntimeResult result)
            implements RuntimeEvent {}

    record Completed(UUID executionId, Instant occurredAt, RuntimeResult result)
            implements RuntimeEvent {}

    record Failed(UUID executionId, Instant occurredAt, String message) implements RuntimeEvent {}

    record Cancelled(UUID executionId, Instant occurredAt) implements RuntimeEvent {}
}
