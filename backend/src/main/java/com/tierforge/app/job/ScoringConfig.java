package com.tierforge.app.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scoring_configs")
public class ScoringConfig {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "footfall_bar", nullable = false)
    private int footfallBar;

    @Column(name = "footfall_weight", nullable = false)
    private int footfallWeight;

    @Column(name = "revenue_bar", nullable = false)
    private double revenueBar;

    @Column(name = "revenue_weight", nullable = false)
    private int revenueWeight;

    @Column(name = "size_bar", nullable = false)
    private int sizeBar;

    @Column(name = "size_weight", nullable = false)
    private int sizeWeight;

    @Column(name = "tier_large_threshold", nullable = false)
    private int tierLargeThreshold;

    @Column(name = "tier_medium_threshold", nullable = false)
    private int tierMediumThreshold;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ScoringConfig() {
    }

    public ScoringConfig(
            UUID id,
            UUID jobId,
            int footfallBar,
            int footfallWeight,
            double revenueBar,
            int revenueWeight,
            int sizeBar,
            int sizeWeight,
            int tierLargeThreshold,
            int tierMediumThreshold,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.jobId = jobId;
        this.footfallBar = footfallBar;
        this.footfallWeight = footfallWeight;
        this.revenueBar = revenueBar;
        this.revenueWeight = revenueWeight;
        this.sizeBar = sizeBar;
        this.sizeWeight = sizeWeight;
        this.tierLargeThreshold = tierLargeThreshold;
        this.tierMediumThreshold = tierMediumThreshold;
        this.updatedAt = updatedAt;
    }

    public void update(
            int footfallBar,
            int footfallWeight,
            double revenueBar,
            int revenueWeight,
            int sizeBar,
            int sizeWeight,
            int tierLargeThreshold,
            int tierMediumThreshold,
            OffsetDateTime updatedAt) {
        this.footfallBar = footfallBar;
        this.footfallWeight = footfallWeight;
        this.revenueBar = revenueBar;
        this.revenueWeight = revenueWeight;
        this.sizeBar = sizeBar;
        this.sizeWeight = sizeWeight;
        this.tierLargeThreshold = tierLargeThreshold;
        this.tierMediumThreshold = tierMediumThreshold;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public int getFootfallBar() {
        return footfallBar;
    }

    public int getFootfallWeight() {
        return footfallWeight;
    }

    public double getRevenueBar() {
        return revenueBar;
    }

    public int getRevenueWeight() {
        return revenueWeight;
    }

    public int getSizeBar() {
        return sizeBar;
    }

    public int getSizeWeight() {
        return sizeWeight;
    }

    public int getTierLargeThreshold() {
        return tierLargeThreshold;
    }

    public int getTierMediumThreshold() {
        return tierMediumThreshold;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
