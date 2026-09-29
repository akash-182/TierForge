package com.tierforge.app.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfig, UUID> {

    Optional<ScoringConfig> findByJobId(UUID jobId);
}
