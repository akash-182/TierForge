package com.tierforge.app.web;

import com.tierforge.app.job.Job;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id, String status, String sourceFilename, int totalStoreCount, OffsetDateTime createdAt) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(), job.getStatus().name(), job.getSourceFilename(), job.getTotalStoreCount(),
                job.getCreatedAt());
    }
}
