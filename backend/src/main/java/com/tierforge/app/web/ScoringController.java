package com.tierforge.app.web;

import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.ScoreQueryDao;
import com.tierforge.app.job.ScoringConfig;
import com.tierforge.app.job.ScoringConfigRepository;
import com.tierforge.app.job.StoreScoreRepository;
import com.tierforge.app.job.StoreTier;
import com.tierforge.app.scoring.ScoringConfigInput;
import com.tierforge.app.scoring.ScoringService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs/{id}")
public class ScoringController {

    private final JobRepository jobRepository;
    private final ScoringService scoringService;
    private final ScoringConfigRepository scoringConfigRepository;
    private final StoreScoreRepository storeScoreRepository;
    private final ScoreQueryDao scoreQueryDao;

    public ScoringController(
            JobRepository jobRepository,
            ScoringService scoringService,
            ScoringConfigRepository scoringConfigRepository,
            StoreScoreRepository storeScoreRepository,
            ScoreQueryDao scoreQueryDao) {
        this.jobRepository = jobRepository;
        this.scoringService = scoringService;
        this.scoringConfigRepository = scoringConfigRepository;
        this.storeScoreRepository = storeScoreRepository;
        this.scoreQueryDao = scoreQueryDao;
    }

    @PostMapping("/scoring")
    public ResponseEntity<ScoringSummaryResponse> submitScoring(
            @PathVariable UUID id, @RequestBody ScoringConfigInput input) {
        requireJobExists(id);
        ScoringConfig config = scoringService.submitConfigAndScore(id, input);
        return ResponseEntity.ok(buildSummary(id, config));
    }

    @GetMapping("/scoring")
    public ResponseEntity<ScoringSummaryResponse> getScoring(@PathVariable UUID id) {
        requireJobExists(id);
        ScoringConfig config =
                scoringConfigRepository.findByJobId(id).orElseThrow(() -> new ScoringNotConfiguredException(id));
        return ResponseEntity.ok(buildSummary(id, config));
    }

    @GetMapping("/scores")
    public ResponseEntity<List<ScoredStoreResponse>> listScores(
            @PathVariable UUID id, @RequestParam(required = false) StoreTier tier) {
        requireJobExists(id);
        List<ScoredStoreResponse> scores = scoreQueryDao.listScores(id, tier).stream()
                .map(ScoredStoreResponse::from)
                .toList();
        return ResponseEntity.ok(scores);
    }

    private void requireJobExists(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new JobNotFoundException(id);
        }
    }

    private ScoringSummaryResponse buildSummary(UUID jobId, ScoringConfig config) {
        TierBreakdown breakdown = TierBreakdown.from(storeScoreRepository.countByTierForJob(jobId));
        int scoredStoreCount = (int) (breakdown.large() + breakdown.medium() + breakdown.small());
        return new ScoringSummaryResponse(ScoringConfigResponse.from(config), breakdown, scoredStoreCount);
    }
}
