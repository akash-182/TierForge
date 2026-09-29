package com.tierforge.app.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.tierforge.app.AbstractIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StoreUnitRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Test
    void savesAndCountsStoreUnitsByJob() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 2, OffsetDateTime.now()));

        storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST000001", "Store One", "1 Main St", "Delhi", "Delhi", "India",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));
        storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST000002", "Store Two", "2 Main St", "Delhi", "Delhi", "India",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));

        assertThat(storeUnitRepository.countByJobId(job.getId())).isEqualTo(2);
    }
}
