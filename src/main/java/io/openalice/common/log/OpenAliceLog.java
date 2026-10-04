package io.openalice.common.log;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class OpenAliceLog {

    private static final Pattern EVENT_NAME =
            Pattern.compile("[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*");
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "authorization",
            "apikey",
            "cookie",
            "usermessage",
            "prompt",
            "memory",
            "memorycontent",
            "request",
            "response",
            "input",
            "output",
            "modelinput",
            "modeloutput",
            "filecontent",
            "toolargs",
            "toolparameters",
            "requestbody",
            "responsebody");
    private static final AtomicReference<LogWriter> WRITER =
            new AtomicReference<>(new Slf4jLogWriter());

    private OpenAliceLog() {}

    public static EventBuilder event(String event) {
        if (event == null || !EVENT_NAME.matcher(event).matches()) {
            throw new IllegalArgumentException(
                    "Log event must use stable <domain>.<event> lowercase naming");
        }
        return new EventBuilder(event);
    }

    public static void configureWriter(LogWriter writer) {
        WRITER.set(Objects.requireNonNull(writer, "writer"));
    }

    public static final class EventBuilder {
        private final String event;
        private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        private String message = "";

        private EventBuilder(String event) {
            this.event = event;
        }

        public EventBuilder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public EventBuilder field(String name, Object value) {
            validateFieldName(name);
            if (value != null) {
                fields.put(name, value);
            }
            return this;
        }

        public EventBuilder fields(Map<String, ?> values) {
            Objects.requireNonNull(values, "values").forEach(this::field);
            return this;
        }

        public void info() {
            write(LogWriter.Level.INFO, null);
        }

        public void warn() {
            write(LogWriter.Level.WARN, null);
        }

        public void error() {
            write(LogWriter.Level.ERROR, null);
        }

        public void error(Throwable error) {
            write(LogWriter.Level.ERROR, Objects.requireNonNull(error, "error"));
        }

        private void write(LogWriter.Level level, Throwable error) {
            LinkedHashMap<String, Object> combined = new LinkedHashMap<>(fields);
            LogContext.current().fields().forEach(combined::put);
            WRITER.get().write(
                    level, new LogWriter.LogRecord(event, message, combined), error);
        }
    }

    private static void validateFieldName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Log field name must not be blank");
        }
        String normalized = name.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (SENSITIVE_FIELD_NAMES.contains(normalized)) {
            throw new IllegalArgumentException("Sensitive payload field is not allowed: " + name);
        }
    }
}
