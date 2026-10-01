package com.tierforge.app.monitor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import java.time.Instant;

class LogBufferAppender extends AppenderBase<ILoggingEvent> {

    private static final String OWN_PACKAGE = "com.tierforge.app.monitor";

    private final LogBuffer buffer;

    LogBufferAppender(LogBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Don't capture the monitor's own logging, or polling it could feed itself.
        if (event.getLoggerName().startsWith(OWN_PACKAGE)) {
            return;
        }
        String message = event.getFormattedMessage();
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            message += " [" + throwable.getClassName() + ": " + throwable.getMessage() + "]";
        }
        String logger = event.getLoggerName();
        buffer.add(
                Instant.ofEpochMilli(event.getTimeStamp()),
                event.getLevel().toString(),
                logger.substring(logger.lastIndexOf('.') + 1),
                message);
    }
}
