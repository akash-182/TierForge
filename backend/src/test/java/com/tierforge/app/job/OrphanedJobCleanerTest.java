package com.tierforge.app.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.tierforge.app.AbstractIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrphanedJobCleanerTest extends AbstractIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private OrphanedJobCleaner cleaner;

    @Test
    void failsRunningJobsAndTheirUnfinishedUnitsOnly() {
        Job running = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "a.csv", 2, OffsetDateTime.now()));
        StoreUnit inProgress = unit(running, "ST1", StoreUnitStatus.IN_PROGRESS);
        StoreUnit done = unit(running, "ST2", StoreUnitStatus.SUCCEEDED);
        Job completed = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.COMPLETED, "b.csv", 1, OffsetDateTime.now()));
        StoreUnit untouched = unit(completed, "ST3", StoreUnitStatus.SUCCEEDED);

        cleaner.failOrphanedJobs();

        assertThat(jobRepository.findById(running.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(storeUnitRepository.findById(inProgress.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreUnitStatus.FAILED);
        assertThat(storeUnitRepository.findById(done.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreUnitStatus.SUCCEEDED);
        assertThat(jobRepository.findById(completed.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.COMPLETED);
        assertThat(storeUnitRepository.findById(untouched.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreUnitStatus.SUCCEEDED);
    }

    private StoreUnit unit(Job job, String storeId, StoreUnitStatus status) {
        return storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, storeId, "A", "Addr", "City", "State", "Country",
                status, OffsetDateTime.now()));
    }
}
