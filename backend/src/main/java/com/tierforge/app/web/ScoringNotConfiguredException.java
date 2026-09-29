package com.tierforge.app.web;

import java.util.UUID;

public class ScoringNotConfiguredException extends RuntimeException {

    public ScoringNotConfiguredException(UUID jobId) {
        super("No scoring config submitted yet for job: " + jobId);
    }
}
