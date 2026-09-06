package openalice.core.domain;

import java.time.Instant;
import java.util.Map;

public record MemoryRecord(
        String id,
        String content,
        Map<String, String> metadata,
        Instant createdAt
) {
}

