package com.tierforge.app.job;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StoreUnitRepository extends JpaRepository<StoreUnit, UUID> {

    long countByJobId(UUID jobId);

    long countByJobIdAndStatusIn(UUID jobId, List<StoreUnitStatus> statuses);

    // Units already claimed whose lease hasn't expired yet - i.e. genuinely still "in flight"
    // (either mid-call or queued for a rate-limiter permit), as opposed to claimed-but-abandoned.
    // Used to cap how much new work gets claimed per poll tick - see the comment on
    // EnrichmentOrchestrator's claim-sizing logic for why this matters.
    long countByJobIdAndStatusAndLeaseExpiresAtAfter(UUID jobId, StoreUnitStatus status, OffsetDateTime now);

    List<StoreUnit> findByJobId(UUID jobId);

    List<StoreUnit> findByJobIdAndStatus(UUID jobId, StoreUnitStatus status);

    @Query("SELECT su.status AS status, COUNT(su) AS count FROM StoreUnit su WHERE su.job.id = :jobId GROUP BY su.status")
    List<StatusCount> countByStatusForJob(@Param("jobId") UUID jobId);

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE store_units SET status = 'SUCCEEDED', last_error = NULL "
                    + "WHERE id = :id AND lease_token = :leaseToken",
            nativeQuery = true)
    int markSucceeded(@Param("id") UUID id, @Param("leaseToken") UUID leaseToken);

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE store_units SET "
                    + "status = CASE WHEN attempt_count >= :maxAttempts THEN 'FAILED' ELSE 'PENDING' END, "
                    + "last_error = :error "
                    + "WHERE id = :id AND lease_token = :leaseToken",
            nativeQuery = true)
    int markFailedOrRequeue(
            @Param("id") UUID id,
            @Param("leaseToken") UUID leaseToken,
            @Param("error") String error,
            @Param("maxAttempts") int maxAttempts);

    interface StatusCount {
        StoreUnitStatus getStatus();

        long getCount();
    }
}
