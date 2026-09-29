package com.tierforge.app.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.tierforge.app.AbstractIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnrichmentResultRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private EnrichmentResultRepository enrichmentResultRepository;

    @Test
    void savesAndFindsResultByStoreUnitId() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 1, OffsetDateTime.now()));
        StoreUnit unit = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST000001", "Store One", "1 Main St", "Delhi", "Delhi", "India",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));

        enrichmentResultRepository.save(new EnrichmentResult(
                UUID.randomUUID(), unit.getId(), 18234, 142033.50, 6210, OffsetDateTime.now()));

        var found = enrichmentResultRepository.findByStoreUnitId(unit.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEstimatedMonthlyFootfall()).isEqualTo(18234);
        assertThat(found.get().getEstimatedMonthlyRevenue()).isEqualTo(142033.50);
        assertThat(found.get().getStoreSizeSqft()).isEqualTo(6210);
    }
}
