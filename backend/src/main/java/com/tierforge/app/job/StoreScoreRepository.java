package com.tierforge.app.job;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StoreScoreRepository extends JpaRepository<StoreScore, UUID> {

    // A bulk @Modifying delete, not the derived deleteBy... form: the derived form queues entity
    // removal in the persistence context, and Hibernate's default flush order runs all inserts
    // before deletes regardless of call order — so a same-transaction re-insert for the same
    // store_unit_id would violate its unique constraint before the old row was actually gone.
    // @Modifying executes immediately as a JDBC statement, sidestepping that ordering entirely.
    @Modifying
    @Transactional
    @Query("DELETE FROM StoreScore s WHERE s.storeUnitId IN :storeUnitIds")
    void deleteByStoreUnitIdIn(@Param("storeUnitIds") List<UUID> storeUnitIds);

    @Query("SELECT s.tier AS tier, COUNT(s) AS count FROM StoreScore s WHERE s.jobId = :jobId GROUP BY s.tier")
    List<TierCount> countByTierForJob(@Param("jobId") UUID jobId);

    interface TierCount {
        StoreTier getTier();

        long getCount();
    }
}
