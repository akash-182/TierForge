package com.tierforge.app.job;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Uses JdbcTemplate directly (not Spring Data JPA) because this needs UPDATE ... RETURNING to
// map the claimed rows back to ClaimedUnit DTOs in one round trip — Spring Data JPA's
// @Modifying queries execute via JPA's bulk-update path, which only ever returns a row count,
// discarding any RETURNING result set.
@Repository
public class StoreUnitClaimDao {

    private final JdbcTemplate jdbcTemplate;

    public StoreUnitClaimDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Claims up to {@code maxToClaim} eligible units. The caller is responsible for keeping
     * {@code maxToClaim} in line with how many units the rate limiter can actually service
     * within one lease window (see EnrichmentOrchestrator) — claiming more than that just means
     * units sit queued for a permit until their lease expires, get reclaimed before ever
     * attempting a real call, and eventually exhaust attempt_count without ever producing a
     * genuine outcome, leaving them permanently stuck (unclaimable, never marked FAILED).
     */
    public List<ClaimedUnit> claimBatch(UUID jobId, long leaseSeconds, int maxAttempts, int maxToClaim) {
        if (maxToClaim <= 0) {
            return List.of();
        }
        String sql = """
                UPDATE store_units
                SET status = 'IN_PROGRESS',
                    lease_token = gen_random_uuid(),
                    claimed_at = now(),
                    lease_expires_at = now() + make_interval(secs => ?),
                    attempt_count = attempt_count + 1
                WHERE id IN (
                    SELECT id FROM store_units
                    WHERE job_id = ?
                      AND attempt_count < ?
                      AND (status = 'PENDING' OR (status = 'IN_PROGRESS' AND lease_expires_at < now()))
                    ORDER BY claimed_at ASC NULLS FIRST
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, lease_token, store_id, store_name, address, city, state
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ClaimedUnit(
                        rs.getObject("id", UUID.class),
                        rs.getObject("lease_token", UUID.class),
                        rs.getString("store_id"),
                        rs.getString("store_name"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getString("state")),
                leaseSeconds,
                jobId,
                maxAttempts,
                maxToClaim);
    }
}
