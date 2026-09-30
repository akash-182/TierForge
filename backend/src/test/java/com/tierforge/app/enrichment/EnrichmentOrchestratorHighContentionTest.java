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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Reproduces, at a small but realistic scale, a bug where claiming every eligible unit in one
 * shot (with no cap) meant most units sat queued for a rate-limiter permit until their lease
 * expired before ever attempting a real call. They'd get reclaimed (bumping attempt_count)
 * without a genuine outcome, and once attempts ran out they were permanently stuck: unclaimable,
 * but never marked FAILED either. A real 5,000-store job at production rate-limit settings hit
 * this in practice; the test suite never caught it because the fast test profile's rate limit
 * (1000/sec) is high enough that a batch always drains well within one lease window.
 *
 * This test overrides the rate limit and lease duration to reliably reproduce the same
 * imbalance — more claimable units than one lease window can drain — at a scale that still runs
 * in a couple of seconds.
 */
@TestPropertySource(properties = {
        "tierforge.enrichment.rate-limit-per-second=2",
        "tierforge.enrichment.lease-duration=1s",
        "tierforge.enrichment.orchestrator-poll-interval=100ms",
        "tierforge.enrichment.max-attempts=5"
})
class EnrichmentOrchestratorHighContentionTest extends AbstractIntegrationTest {

    @Autowired
    private EnrichmentOrchestrator orchestrator;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @MockBean
    private EnrichmentClient enrichmentClient;

    @Test
    void completesWhenFarMoreUnitsAreEligibleThanTheRateLimiterCanDrainPerLeaseWindow() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 10, OffsetDateTime.now()));
        for (int i = 0; i < 10; i++) {
            storeUnitRepository.save(new StoreUnit(
                    UUID.randomUUID(), job, "ST" + i, "Store " + i, "Addr", "City", "State", "Country",
                    StoreUnitStatus.PENDING, OffsetDateTime.now()));
        }
        // 10 units, but only 2/sec * 1s lease = 2 unit-slots "safe" per window - a 5x overhang
        // that would starve most units under the old unbounded-claim behavior.
        when(enrichmentClient.enrich(any())).thenReturn(new EnrichResult(1000, 5000.0, 2000));

        orchestrator.startJob(job.getId());

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Job reloaded = jobRepository.findById(job.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });

        assertThat(storeUnitRepository.countByJobIdAndStatusIn(job.getId(), List.of(StoreUnitStatus.SUCCEEDED)))
                .isEqualTo(10);
        // No unit should have been forced to burn all 5 attempts just from reclaim churn while
        // waiting for a permit - each mocked call succeeds on its first real attempt.
        for (StoreUnit unit : storeUnitRepository.findByJobId(job.getId())) {
            assertThat(unit.getAttemptCount()).isLessThanOrEqualTo(2);
        }
    }
}
