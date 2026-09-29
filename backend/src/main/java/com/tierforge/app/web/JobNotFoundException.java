package com.tierforge.app.web;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    private final UUID jobId;

    public JobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId);
        this.jobId = jobId;
    }

    public UUID getJobId() {
        return jobId;
    }
}
