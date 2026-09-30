package com.tierforge.app.enrichment;

import com.tierforge.app.job.ClaimedUnit;
import com.tierforge.app.job.EnrichmentResult;
import com.tierforge.app.job.EnrichmentResultRepository;
import com.tierforge.app.job.StoreUnitRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentWorker.class);

    private final StoreUnitRepository storeUnitRepository;
    private final EnrichmentResultRepository enrichmentResultRepository;
    private final EnrichmentClient enrichmentClient;
    private final RateLimiter rateLimiter;
    private final EnrichmentProperties properties;

    public EnrichmentWorker(
            StoreUnitRepository storeUnitRepository,
            EnrichmentResultRepository enrichmentResultRepository,
            EnrichmentClient enrichmentClient,
            RateLimiter rateLimiter,
            EnrichmentProperties properties) {
        this.storeUnitRepository = storeUnitRepository;
        this.enrichmentResultRepository = enrichmentResultRepository;
        this.enrichmentClient = enrichmentClient;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    public void processClaimedUnit(ClaimedUnit unit) {
        try {
            RateLimiter.waitForPermission(rateLimiter);
            EnrichResult result = enrichmentClient.enrich(new EnrichRequest(
                    unit.storeId(), unit.storeName(), unit.address(), unit.city(), unit.state()));
            applySuccess(unit.id(), unit.leaseToken(), result);
        } catch (EnrichmentCallException e) {
            log.warn("Enrichment call failed for store {} (unit {}): {}", unit.storeId(), unit.id(), e.getMessage());
            applyFailureOrRequeue(unit.id(), unit.leaseToken(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error enriching store {} (unit {})", unit.storeId(), unit.id(), e);
            applyFailureOrRequeue(unit.id(), unit.leaseToken(), "Unexpected error: " + e.getMessage());
        }
    }

    @Transactional
    void applySuccess(UUID unitId, UUID leaseToken, EnrichResult result) {
        int updated = storeUnitRepository.markSucceeded(unitId, leaseToken);
        if (updated == 1) {
            enrichmentResultRepository.save(new EnrichmentResult(
                    UUID.randomUUID(), unitId, result.footfall(), result.revenue(), result.sqft(),
                    OffsetDateTime.now()));
            log.debug("Unit {} succeeded", unitId);
        } else {
            log.debug("Unit {} succeeded but lease token was stale - late response discarded", unitId);
        }
    }

    void applyFailureOrRequeue(UUID unitId, UUID leaseToken, String error) {
        int updated = storeUnitRepository.markFailedOrRequeue(
                unitId,
                leaseToken,
                error,
                properties.maxAttempts(),
                properties.retryBackoffBase().toMillis() / 1000.0,
                properties.retryBackoffMax().toMillis() / 1000.0);
        if (updated != 1) {
            log.debug("Unit {} failure recorded but lease token was stale - late response discarded", unitId);
        }
    }
}
