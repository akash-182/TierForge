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
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "source_filename", nullable = false)
    private String sourceFilename;

    @Column(name = "total_store_count", nullable = false)
    private int totalStoreCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Job() {
    }

    public Job(UUID id, JobStatus status, String sourceFilename, int totalStoreCount, OffsetDateTime createdAt) {
        this.id = id;
        this.status = status;
        this.sourceFilename = sourceFilename;
        this.totalStoreCount = totalStoreCount;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public int getTotalStoreCount() {
        return totalStoreCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
