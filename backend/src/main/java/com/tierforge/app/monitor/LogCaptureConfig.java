package com.tierforge.app.monitor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

// Attaches the in-memory appender to the app's logger programmatically, so the existing logging
// configuration is left untouched.
@Configuration
public class LogCaptureConfig {

    private final LogBuffer buffer;
    private LogBufferAppender appender;
    private Logger appLogger;

    public LogCaptureConfig(LogBuffer buffer) {
        this.buffer = buffer;
    }

    @PostConstruct
    void attach() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof LoggerContext context)) {
            return;
        }
        appender = new LogBufferAppender(buffer);
        appender.setContext(context);
        appender.setName("monitor-log-buffer");
        appender.start();
        appLogger = context.getLogger("com.tierforge.app");
        appLogger.addAppender(appender);
    }

    @PreDestroy
    void detach() {
        if (appLogger != null && appender != null) {
            appLogger.detachAppender(appender);
            appender.stop();
        }
    }
}
