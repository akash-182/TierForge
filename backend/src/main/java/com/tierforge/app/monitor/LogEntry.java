package com.tierforge.app.monitor;

import java.time.Instant;

public record LogEntry(long seq, Instant timestamp, String level, String logger, String message) {
}
