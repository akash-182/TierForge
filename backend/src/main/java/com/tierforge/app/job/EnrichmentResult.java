package com.tierforge.app.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "enrichment_results")
public class EnrichmentResult {

    @Id
    private UUID id;

    @Column(name = "store_unit_id", nullable = false, unique = true)
    private UUID storeUnitId;

    @Column(name = "estimated_monthly_footfall", nullable = false)
    private int estimatedMonthlyFootfall;

    @Column(name = "estimated_monthly_revenue", nullable = false)
    private double estimatedMonthlyRevenue;

    @Column(name = "store_size_sqft", nullable = false)
    private int storeSizeSqft;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    protected EnrichmentResult() {
    }

    public EnrichmentResult(
            UUID id,
            UUID storeUnitId,
            int estimatedMonthlyFootfall,
            double estimatedMonthlyRevenue,
            int storeSizeSqft,
            OffsetDateTime receivedAt) {
        this.id = id;
        this.storeUnitId = storeUnitId;
        this.estimatedMonthlyFootfall = estimatedMonthlyFootfall;
        this.estimatedMonthlyRevenue = estimatedMonthlyRevenue;
        this.storeSizeSqft = storeSizeSqft;
        this.receivedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoreUnitId() {
        return storeUnitId;
    }

    public int getEstimatedMonthlyFootfall() {
        return estimatedMonthlyFootfall;
    }

    public double getEstimatedMonthlyRevenue() {
        return estimatedMonthlyRevenue;
    }

    public int getStoreSizeSqft() {
        return storeSizeSqft;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
}
