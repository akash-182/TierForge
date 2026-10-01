package com.tierforge.app.monitor;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

// Keeps only the most recent log lines in memory so the live monitor can poll them
// incrementally by sequence number. Process-local: lost on restart.
@Component
public class LogBuffer {

    static final int CAPACITY = 500;

    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private long lastSeq = 0;

    public synchronized void add(Instant timestamp, String level, String logger, String message) {
        entries.addLast(new LogEntry(++lastSeq, timestamp, level, logger, message));
        while (entries.size() > CAPACITY) {
            entries.removeFirst();
        }
    }

    /** Entries with seq greater than {@code afterSeq}, oldest first, at most {@code limit}. */
    public synchronized List<LogEntry> since(long afterSeq, int limit) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (entry.seq() > afterSeq) {
                result.add(entry);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    public synchronized long lastSeq() {
        return lastSeq;
    }
}
