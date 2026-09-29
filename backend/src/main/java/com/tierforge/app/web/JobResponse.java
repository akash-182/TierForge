package com.tierforge.app.web;

import com.tierforge.app.job.Job;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String status,
        String sourceFilename,
        int totalStoreCount,
        OffsetDateTime createdAt,
        long pending,
        long inProgress,
        long succeeded,
        long failed) {

    public static JobResponse from(Job job, StoreUnitCounts counts) {
        return new JobResponse(
                job.getId(), job.getStatus().name(), job.getSourceFilename(), job.getTotalStoreCount(),
                job.getCreatedAt(), counts.pending(), counts.inProgress(), counts.succeeded(), counts.failed());
    }
}
