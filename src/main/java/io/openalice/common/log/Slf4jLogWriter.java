package io.openalice.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

public final class Slf4jLogWriter implements LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger("io.openalice.events");

    @Override
    public void write(Level level, LogRecord record, Throwable error) {
        LoggingEventBuilder builder = switch (level) {
            case INFO -> LOGGER.atInfo();
            case WARN -> LOGGER.atWarn();
            case ERROR -> LOGGER.atError();
        };
        builder.addKeyValue("event", record.event());
        record.fields().forEach(builder::addKeyValue);
        if (error != null) {
            builder.setCause(error);
        }
        builder.log(format(record));
    }

    private static String format(LogRecord record) {
        StringBuilder output = new StringBuilder("event=").append(record.event());
        if (!record.message().isBlank()) {
            output.append(" message=\"").append(safe(record.message())).append('"');
        }
        record.fields().forEach((name, value) -> output.append(' ')
                .append(name)
                .append('=')
                .append(safe(String.valueOf(value))));
        return output.toString();
    }

    private static String safe(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
