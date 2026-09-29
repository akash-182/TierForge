package com.tierforge.app.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrichmentResultRepository extends JpaRepository<EnrichmentResult, UUID> {

    Optional<EnrichmentResult> findByStoreUnitId(UUID storeUnitId);
}
