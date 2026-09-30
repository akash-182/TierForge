package com.tierforge.app.enrichment;

import com.tierforge.app.job.ClaimedUnit;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnitClaimDao;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrichmentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentOrchestrator.class);

    private final StoreUnitClaimDao claimDao;
    private final StoreUnitRepository storeUnitRepository;
    private final JobRepository jobRepository;
    private final EnrichmentWorker worker;
    private final EnrichmentProperties properties;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<UUID> runningJobs = ConcurrentHashMap.newKeySet();

    public EnrichmentOrchestrator(
            StoreUnitClaimDao claimDao,
            StoreUnitRepository storeUnitRepository,
            JobRepository jobRepository,
            EnrichmentWorker worker,
            EnrichmentProperties properties) {
        this.claimDao = claimDao;
        this.storeUnitRepository = storeUnitRepository;
        this.jobRepository = jobRepository;
        this.worker = worker;
        this.properties = properties;
    }

    /**
     * Starts the background processing loop for a job. Returns false if a loop for this job is
     * already running, guarding against a duplicate /start request double-processing.
     */
    public boolean startJob(UUID jobId) {
        if (!runningJobs.add(jobId)) {
            log.warn("startJob({}) called while a processing loop for this job is already running - ignoring",
                    jobId);
            return false;
        }
        log.info("Starting enrichment processing loop for job {}", jobId);
        Thread.ofVirtual().name("job-orchestrator-" + jobId).start(() -> runLoop(jobId));
        return true;
    }

    private void runLoop(UUID jobId) {
        try {
            while (true) {
                // Cap how much we claim per tick at roughly what the rate limiter can drain
                // within one lease window. Claiming unboundedly (e.g. all 5,000 units of a big
                // job in one shot) means most of them just sit queued for a permit until their
                // lease expires before ever attempting a real call — they get reclaimed
                // (bumping attempt_count) without ever producing a genuine outcome, and once
                // attempts run out they're permanently stuck: unclaimable, but never marked
                // FAILED either, since nothing ever wrote a real result for them.
                int targetInFlight =
                        Math.max(1, properties.rateLimitPerSecond() * (int) properties.leaseDuration().toSeconds());
                long currentInFlight = storeUnitRepository.countByJobIdAndStatusAndLeaseExpiresAtAfter(
                        jobId, StoreUnitStatus.IN_PROGRESS, OffsetDateTime.now());
                int room = (int) Math.max(0, targetInFlight - currentInFlight);

                List<ClaimedUnit> claimed = claimDao.claimBatch(
                        jobId, properties.leaseDuration().toSeconds(), properties.maxAttempts(), room);
                if (!claimed.isEmpty()) {
                    log.debug("Job {}: claimed {}/{} units this tick (targetInFlight={}, currentInFlight={})",
                            jobId, claimed.size(), room, targetInFlight, currentInFlight);
                }
                for (ClaimedUnit unit : claimed) {
                    virtualThreadExecutor.execute(() -> worker.processClaimedUnit(unit));
                }

                long remaining = storeUnitRepository.countByJobIdAndStatusIn(
                        jobId, List.of(StoreUnitStatus.PENDING, StoreUnitStatus.IN_PROGRESS));
                if (remaining == 0) {
                    completeJob(jobId);
                    return;
                }

                Thread.sleep(properties.orchestratorPollInterval());
            }
        } catch (InterruptedException e) {
            log.warn("Enrichment processing loop for job {} was interrupted", jobId);
            Thread.currentThread().interrupt();
        } finally {
            runningJobs.remove(jobId);
        }
    }

    @Transactional
    void completeJob(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);
        log.info("Job {} completed", jobId);
    }
}
