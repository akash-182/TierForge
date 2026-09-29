package com.tierforge.app.web;

import com.tierforge.app.job.JobStatus;
import java.util.UUID;

public class JobNotStartableException extends RuntimeException {

    public JobNotStartableException(UUID jobId, JobStatus currentStatus) {
        super("Job " + jobId + " cannot be started: current status is " + currentStatus);
    }
}
