package com.tierforge.app.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.EnrichmentResult;
import com.tierforge.app.job.EnrichmentResultRepository;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.ScoreQueryDao;
import com.tierforge.app.job.ScoredStore;
import com.tierforge.app.job.ScoringConfig;
import com.tierforge.app.job.StoreTier;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ScoringServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private EnrichmentResultRepository enrichmentResultRepository;

    @Autowired
    private ScoreQueryDao scoreQueryDao;

    private Job createJob(int storeCount) {
        return jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.COMPLETED, "stores.csv", storeCount, OffsetDateTime.now()));
    }

    private StoreUnit succeededUnit(Job job, String storeId, String storeName) {
        return storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, storeId, storeName, "Addr", "City", "State", "Country",
                StoreUnitStatus.SUCCEEDED, OffsetDateTime.now()));
    }

    private void seedResult(UUID storeUnitId, int footfall, double revenue, int sqft) {
        enrichmentResultRepository.save(
                new EnrichmentResult(UUID.randomUUID(), storeUnitId, footfall, revenue, sqft, OffsetDateTime.now()));
    }

    @Test
    void rejectsWeightsThatDoNotSumTo100() {
        Job job = createJob(0);
        ScoringConfigInput input =
                new ScoringConfigInput(15000, 50, 150000, 30, 8000, 30, 70, 40); // sums to 110

        assertThatThrownBy(() -> scoringService.submitConfigAndScore(job.getId(), input))
                .isInstanceOf(ScoringValidationException.class)
                .hasMessageContaining("100");
    }

    @Test
    void rejectsLargeThresholdBelowMediumThreshold() {
        Job job = createJob(0);
        ScoringConfigInput input = new ScoringConfigInput(15000, 50, 150000, 30, 8000, 20, 30, 40);

        assertThatThrownBy(() -> scoringService.submitConfigAndScore(job.getId(), input))
                .isInstanceOf(ScoringValidationException.class);
    }

    /**
     * Exact scenario from the exercise spec's worked example (Section 4, Step 3): bars
     * footfall>=15000 / revenue>=150000 / size>=8000, weights 50/30/20, tiers Large>=70 /
     * Medium>=40. Store A clears all three (100%, Large), Store B clears only footfall (50%,
     * Medium), Store C clears only size (20%, Small) despite clearing a bar at all.
     */
    @Test
    void matchesTheSpecWorkedExample() {
        Job job = createJob(3);
        StoreUnit a = succeededUnit(job, "A", "Store A");
        StoreUnit b = succeededUnit(job, "B", "Store B");
        StoreUnit c = succeededUnit(job, "C", "Store C");
        seedResult(a.getId(), 40_000, 400_000, 18_000);
        seedResult(b.getId(), 20_000, 100_000, 5_000);
        seedResult(c.getId(), 2_000, 20_000, 9_000);

        ScoringConfigInput input = new ScoringConfigInput(15_000, 50, 150_000, 30, 8_000, 20, 70, 40);
        scoringService.submitConfigAndScore(job.getId(), input);

        List<ScoredStore> scores = scoreQueryDao.listScores(job.getId(), null);
        var byStoreId = scores.stream().collect(java.util.stream.Collectors.toMap(ScoredStore::storeId, s -> s));

        assertThat(byStoreId.get("A").score()).isEqualTo(100);
        assertThat(byStoreId.get("A").tier()).isEqualTo(StoreTier.LARGE);
        assertThat(byStoreId.get("B").score()).isEqualTo(50);
        assertThat(byStoreId.get("B").tier()).isEqualTo(StoreTier.MEDIUM);
        assertThat(byStoreId.get("C").score()).isEqualTo(20);
        assertThat(byStoreId.get("C").tier()).isEqualTo(StoreTier.SMALL);
    }

    @Test
    void ignoresStoresThatAreNotSucceeded() {
        Job job = createJob(2);
        StoreUnit succeeded = succeededUnit(job, "ST1", "A");
        seedResult(succeeded.getId(), 40_000, 400_000, 18_000);
        storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST2", "B", "Addr", "City", "State", "Country",
                StoreUnitStatus.FAILED, OffsetDateTime.now()));

        ScoringConfigInput input = new ScoringConfigInput(15_000, 50, 150_000, 30, 8_000, 20, 70, 40);
        scoringService.submitConfigAndScore(job.getId(), input);

        assertThat(scoreQueryDao.listScores(job.getId(), null)).hasSize(1);
    }

    @Test
    void reRunningWithDifferentConfigReplacesScores() {
        Job job = createJob(1);
        StoreUnit unit = succeededUnit(job, "ST1", "A");
        seedResult(unit.getId(), 20_000, 100_000, 5_000);

        scoringService.submitConfigAndScore(job.getId(), new ScoringConfigInput(15_000, 50, 150_000, 30, 8_000, 20, 70, 40));
        assertThat(scoreQueryDao.listScores(job.getId(), null).get(0).tier()).isEqualTo(StoreTier.MEDIUM);

        // Loosen the footfall bar so this same store now clears everything it's weighted on.
        ScoringConfig updated = scoringService.submitConfigAndScore(
                job.getId(), new ScoringConfigInput(10_000, 100, 150_000, 0, 8_000, 0, 70, 40));

        assertThat(updated.getFootfallBar()).isEqualTo(10_000);
        List<ScoredStore> scores = scoreQueryDao.listScores(job.getId(), null);
        // "hasSize(1)" here (not just an updated value) is the real assertion that the re-run
        // replaced rather than duplicated this store's score row — a duplicate would either show
        // up as size 2 or throw on store_scores' unique constraint on store_unit_id.
        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).score()).isEqualTo(100);
        assertThat(scores.get(0).tier()).isEqualTo(StoreTier.LARGE);
    }
}
