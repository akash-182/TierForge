package com.tierforge.app.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreUnitRepository extends JpaRepository<StoreUnit, UUID> {

    long countByJobId(UUID jobId);
}
