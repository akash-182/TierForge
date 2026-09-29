package com.tierforge.app.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "store_units",
        uniqueConstraints = @UniqueConstraint(name = "uq_store_units_job_store", columnNames = {"job_id", "store_id"}))
public class StoreUnit {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "store_id", nullable = false, length = 64)
    private String storeId;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String state;

    @Column(nullable = false, length = 120)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreUnitStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "lease_expires_at")
    private OffsetDateTime leaseExpiresAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StoreUnit() {
    }

    public StoreUnit(
            UUID id,
            Job job,
            String storeId,
            String storeName,
            String address,
            String city,
            String state,
            String country,
            StoreUnitStatus status,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.job = job;
        this.storeId = storeId;
        this.storeName = storeName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.status = status;
        this.attemptCount = 0;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public StoreUnitStatus getStatus() {
        return status;
    }

    public void setStatus(StoreUnitStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
