package com.tierforge.app.scoring;

import com.tierforge.app.job.EnrichmentResult;
import com.tierforge.app.job.EnrichmentResultRepository;
import com.tierforge.app.job.ScoringConfig;
import com.tierforge.app.job.ScoringConfigRepository;
import com.tierforge.app.job.StoreScore;
import com.tierforge.app.job.StoreScoreRepository;
import com.tierforge.app.job.StoreTier;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes scores and tiers from data already fetched during enrichment — never calls the
 * enrichment API. Fast and safely re-runnable any time the user changes bars, weights, or tier
 * thresholds: each run fully replaces the job's prior scores.
 */
@Service
public class ScoringService {

    private final StoreUnitRepository storeUnitRepository;
    private final EnrichmentResultRepository enrichmentResultRepository;
    private final ScoringConfigRepository scoringConfigRepository;
    private final StoreScoreRepository storeScoreRepository;

    public ScoringService(
            StoreUnitRepository storeUnitRepository,
            EnrichmentResultRepository enrichmentResultRepository,
            ScoringConfigRepository scoringConfigRepository,
            StoreScoreRepository storeScoreRepository) {
        this.storeUnitRepository = storeUnitRepository;
        this.enrichmentResultRepository = enrichmentResultRepository;
        this.scoringConfigRepository = scoringConfigRepository;
        this.storeScoreRepository = storeScoreRepository;
    }

    @Transactional
    public ScoringConfig submitConfigAndScore(UUID jobId, ScoringConfigInput input) {
        validate(input);

        OffsetDateTime now = OffsetDateTime.now();
        ScoringConfig config = scoringConfigRepository.findByJobId(jobId).orElse(null);
        if (config == null) {
            config = scoringConfigRepository.save(new ScoringConfig(
                    UUID.randomUUID(), jobId, input.footfallBar(), input.footfallWeight(), input.revenueBar(),
                    input.revenueWeight(), input.sizeBar(), input.sizeWeight(), input.tierLargeThreshold(),
                    input.tierMediumThreshold(), now));
        } else {
            config.update(
                    input.footfallBar(), input.footfallWeight(), input.revenueBar(), input.revenueWeight(),
                    input.sizeBar(), input.sizeWeight(), input.tierLargeThreshold(), input.tierMediumThreshold(),
                    now);
        }

        recomputeScores(jobId, config);
        return config;
    }

    private void recomputeScores(UUID jobId, ScoringConfig config) {
        List<StoreUnit> succeeded = storeUnitRepository.findByJobIdAndStatus(jobId, StoreUnitStatus.SUCCEEDED);
        List<UUID> unitIds = succeeded.stream().map(StoreUnit::getId).toList();

        Map<UUID, EnrichmentResult> resultsByUnitId = enrichmentResultRepository.findByStoreUnitIdIn(unitIds)
                .stream()
                .collect(Collectors.toMap(EnrichmentResult::getStoreUnitId, Function.identity()));

        storeScoreRepository.deleteByStoreUnitIdIn(unitIds);

        List<StoreScore> scores = new ArrayList<>();
        for (StoreUnit unit : succeeded) {
            EnrichmentResult result = resultsByUnitId.get(unit.getId());
            if (result == null) {
                continue; // defensive: every SUCCEEDED unit should have a result, but don't fail the whole run on one gap
            }
            int score = scoreFor(result, config);
            StoreTier tier = tierFor(score, config);
            scores.add(new StoreScore(UUID.randomUUID(), jobId, unit.getId(), score, tier, OffsetDateTime.now()));
        }
        storeScoreRepository.saveAll(scores);
    }

    private int scoreFor(EnrichmentResult result, ScoringConfig config) {
        int score = 0;
        if (result.getEstimatedMonthlyFootfall() >= config.getFootfallBar()) {
            score += config.getFootfallWeight();
        }
        if (result.getEstimatedMonthlyRevenue() >= config.getRevenueBar()) {
            score += config.getRevenueWeight();
        }
        if (result.getStoreSizeSqft() >= config.getSizeBar()) {
            score += config.getSizeWeight();
        }
        return score;
    }

    private StoreTier tierFor(int score, ScoringConfig config) {
        if (score >= config.getTierLargeThreshold()) {
            return StoreTier.LARGE;
        }
        if (score >= config.getTierMediumThreshold()) {
            return StoreTier.MEDIUM;
        }
        return StoreTier.SMALL;
    }

    private void validate(ScoringConfigInput input) {
        int weightSum = input.footfallWeight() + input.revenueWeight() + input.sizeWeight();
        if (weightSum != 100) {
            throw new ScoringValidationException("Weights must sum to 100, got " + weightSum);
        }
        if (input.footfallWeight() < 0 || input.revenueWeight() < 0 || input.sizeWeight() < 0) {
            throw new ScoringValidationException("Weights must not be negative");
        }
        if (input.tierLargeThreshold() < 0 || input.tierLargeThreshold() > 100
                || input.tierMediumThreshold() < 0 || input.tierMediumThreshold() > 100) {
            throw new ScoringValidationException("Tier thresholds must be between 0 and 100");
        }
        if (input.tierLargeThreshold() < input.tierMediumThreshold()) {
            throw new ScoringValidationException("tierLargeThreshold must be >= tierMediumThreshold");
        }
    }
}
