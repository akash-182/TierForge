package com.tierforge.app.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class EnrichmentOrchestratorTest extends AbstractIntegrationTest {

    @Autowired
    private EnrichmentOrchestrator orchestrator;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @MockBean
    private EnrichmentClient enrichmentClient;

    @Test
    void runsSmallJobToCompletion() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 3, OffsetDateTime.now()));
        for (int i = 0; i < 3; i++) {
            storeUnitRepository.save(new StoreUnit(
                    UUID.randomUUID(), job, "ST" + i, "Store " + i, "Addr", "City", "State", "Country",
                    StoreUnitStatus.PENDING, OffsetDateTime.now()));
        }
        when(enrichmentClient.enrich(any())).thenReturn(new EnrichResult(1000, 5000.0, 2000));

        boolean started = orchestrator.startJob(job.getId());
        assertThat(started).isTrue();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Job reloaded = jobRepository.findById(job.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });
        assertThat(storeUnitRepository.countByJobIdAndStatusIn(job.getId(), List.of(StoreUnitStatus.SUCCEEDED)))
                .isEqualTo(3);
    }

    @Test
    void reclaimsAndRetriesAHungCall() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 1, OffsetDateTime.now()));
        storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));

        // Test profile sets lease-duration=1500ms; simulate the FIRST call hanging well past
        // that, then succeeding on the SECOND (reclaimed) attempt.
        AtomicInteger callCount = new AtomicInteger(0);
        when(enrichmentClient.enrich(any())).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0) {
                Thread.sleep(3000);
                throw new EnrichmentCallException("simulated hang, client gave up");
            }
            return new EnrichResult(1000, 5000.0, 2000);
        });

        orchestrator.startJob(job.getId());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Job reloaded = jobRepository.findById(job.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });
        StoreUnit finalUnit = storeUnitRepository.findByJobId(job.getId()).get(0);
        assertThat(finalUnit.getStatus()).isEqualTo(StoreUnitStatus.SUCCEEDED);
        assertThat(finalUnit.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void doubleStartOnlyRunsOneLoop() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 0, OffsetDateTime.now()));

        boolean first = orchestrator.startJob(job.getId());
        boolean second = orchestrator.startJob(job.getId());

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
