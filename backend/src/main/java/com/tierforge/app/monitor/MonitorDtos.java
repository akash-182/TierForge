package com.tierforge.app.monitor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MonitorDtos {

    private MonitorDtos() {
    }

    public record JobMonitorRow(
            UUID id,
            String status,
            String sourceFilename,
            int totalStoreCount,
            Instant createdAt,
            Instant lastActivityAt,
            long pending,
            long inProgress,
            long succeeded,
            long failed,
            long retrying,
            long staleLeases,
            double throughputPerSec,
            int percentDone) {
    }

    public record LogsResponse(List<LogEntry> entries, long lastSeq) {
    }

    public record ErrorReason(String reason, long count) {
    }

    public record RecentError(
            UUID jobId, String storeId, int attemptCount, String status, String lastError, Instant lastAttemptAt) {
    }

    public record ErrorsResponse(List<ErrorReason> reasons, List<RecentError> recent) {
    }
}
