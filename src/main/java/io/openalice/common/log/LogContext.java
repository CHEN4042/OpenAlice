package io.openalice.common.log;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import reactor.core.publisher.Hooks;
import reactor.util.context.Context;

public final class LogContext {

    public static final String REACTOR_CONTEXT_KEY = "openalice.log-context";
    public static final String REQUEST_ID = "requestId";
    public static final String EXECUTION_ID = "executionId";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String TURN_ID = "turnId";

    private static final ThreadLocal<LogContext> CURRENT = new ThreadLocal<>();
    private static final AtomicBoolean PROPAGATION_INITIALIZED = new AtomicBoolean();
    private static final LogContext EMPTY = new LogContext(Map.of());

    static {
        initializePropagation();
    }

    private final Map<String, Object> fields;

    private LogContext(Map<String, ?> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LogContext current() {
        LogContext context = CURRENT.get();
        return context == null ? EMPTY : context;
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public LogContext withField(String name, Object value) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(fields);
        putIfPresent(updated, name, value);
        return new LogContext(updated);
    }

    public LogContext withExecutionId(Object executionId) {
        return withField(EXECUTION_ID, executionId);
    }

    public Function<Context, Context> writeToReactorContext() {
        return context -> context.put(REACTOR_CONTEXT_KEY, this);
    }

    public void run(Runnable action) {
        Objects.requireNonNull(action, "action");
        LogContext previous = CURRENT.get();
        try {
            CURRENT.set(this);
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void initializePropagation() {
        if (!PROPAGATION_INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        ContextRegistry.getInstance().registerThreadLocalAccessor(new LogContextAccessor());
        Hooks.enableAutomaticContextPropagation();
    }

    private static void putIfPresent(Map<String, Object> target, String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Log context field name must not be blank");
        }
        if (value != null) {
            target.put(name, value);
        }
    }

    public static final class Builder {
        private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        public Builder requestId(Object requestId) {
            return field(REQUEST_ID, requestId);
        }

        public Builder executionId(Object executionId) {
            return field(EXECUTION_ID, executionId);
        }

        public Builder conversationId(Object conversationId) {
            return field(CONVERSATION_ID, conversationId);
        }

        public Builder turnId(Object turnId) {
            return field(TURN_ID, turnId);
        }

        public Builder field(String name, Object value) {
            putIfPresent(fields, name, value);
            return this;
        }

        public Builder fields(Map<String, ?> values) {
            Objects.requireNonNull(values, "values").forEach(this::field);
            return this;
        }

        public LogContext build() {
            return fields.isEmpty() ? EMPTY : new LogContext(fields);
        }
    }

    private static final class LogContextAccessor implements ThreadLocalAccessor<LogContext> {

        @Override
        public Object key() {
            return REACTOR_CONTEXT_KEY;
        }

        @Override
        public LogContext getValue() {
            return CURRENT.get();
        }

        @Override
        public void setValue(LogContext value) {
            CURRENT.set(value);
        }

        @Override
        public void setValue() {
            CURRENT.remove();
        }
    }
}
