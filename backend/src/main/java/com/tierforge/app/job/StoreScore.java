package com.tierforge.app.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_scores")
public class StoreScore {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "store_unit_id", nullable = false, unique = true)
    private UUID storeUnitId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StoreTier tier;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    protected StoreScore() {
    }

    public StoreScore(UUID id, UUID jobId, UUID storeUnitId, int score, StoreTier tier, OffsetDateTime computedAt) {
        this.id = id;
        this.jobId = jobId;
        this.storeUnitId = storeUnitId;
        this.score = score;
        this.tier = tier;
        this.computedAt = computedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getStoreUnitId() {
        return storeUnitId;
    }

    public int getScore() {
        return score;
    }

    public StoreTier getTier() {
        return tier;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }
}
