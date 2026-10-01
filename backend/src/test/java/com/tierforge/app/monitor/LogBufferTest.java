package com.tierforge.app.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogBufferTest {

    @Test
    void returnsEntriesAfterSequenceInOrder() {
        LogBuffer buffer = new LogBuffer();
        buffer.add(Instant.now(), "INFO", "A", "one");
        buffer.add(Instant.now(), "WARN", "A", "two");
        buffer.add(Instant.now(), "ERROR", "B", "three");

        List<LogEntry> all = buffer.since(0, 10);
        assertThat(all).extracting(LogEntry::message).containsExactly("one", "two", "three");
        assertThat(all).extracting(LogEntry::seq).containsExactly(1L, 2L, 3L);

        assertThat(buffer.since(2, 10)).extracting(LogEntry::message).containsExactly("three");
        assertThat(buffer.since(3, 10)).isEmpty();
    }

    @Test
    void respectsLimitAndKeepsOnlyMostRecentEntries() {
        LogBuffer buffer = new LogBuffer();
        for (int i = 1; i <= LogBuffer.CAPACITY + 20; i++) {
            buffer.add(Instant.now(), "INFO", "A", "m" + i);
        }

        List<LogEntry> all = buffer.since(0, 10_000);
        assertThat(all).hasSize(LogBuffer.CAPACITY);
        assertThat(all.get(0).message()).isEqualTo("m21");
        assertThat(buffer.lastSeq()).isEqualTo(LogBuffer.CAPACITY + 20);
        assertThat(buffer.since(0, 5)).hasSize(5);
    }
}
