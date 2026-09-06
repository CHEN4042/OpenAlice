package openalice.core.port;

import java.util.Map;

public interface TracePort {
    default void record(String event, Map<String, String> metadata) {
        // Phase 1 keeps tracing optional; default implementation avoids forcing adapters.
    }
}

