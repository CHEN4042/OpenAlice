package io.openalice.common.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public interface LogWriter {

    void write(Level level, LogRecord record, Throwable error);

    enum Level {
        INFO,
        WARN,
        ERROR
    }

    record LogRecord(String event, String message, Map<String, Object> fields) {
        public LogRecord {
            event = Objects.requireNonNull(event, "event");
            message = message == null ? "" : message;
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }
}
