package com.tierforge.app.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// The orchestrator's processing loop is in-memory only, so any job still RUNNING when this
// process starts has nothing driving it. Fail those jobs (and their unfinished units) up front
// so they don't linger as RUNNING forever.
@Component
public class OrphanedJobCleaner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrphanedJobCleaner.class);

    private final JdbcTemplate jdbcTemplate;

    public OrphanedJobCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        failOrphanedJobs();
    }

    @Transactional
    public int failOrphanedJobs() {
        jdbcTemplate.update("""
                UPDATE store_units
                SET status = 'FAILED', last_error = 'Orphaned: backend restarted mid-job'
                WHERE status IN ('PENDING', 'IN_PROGRESS')
                  AND job_id IN (SELECT id FROM jobs WHERE status = 'RUNNING')
                """);
        int jobs = jdbcTemplate.update("UPDATE jobs SET status = 'FAILED' WHERE status = 'RUNNING'");
        if (jobs > 0) {
            log.warn("Marked {} orphaned RUNNING job(s) as FAILED on startup", jobs);
        }
        return jobs;
    }
}
