package com.tierforge.app.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.ClaimedUnit;
import com.tierforge.app.job.EnrichmentResultRepository;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitClaimDao;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

class EnrichmentWorkerTest extends AbstractIntegrationTest {

    @Autowired
    private EnrichmentWorker worker;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private StoreUnitClaimDao claimDao;

    @Autowired
    private EnrichmentResultRepository enrichmentResultRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EnrichmentClient enrichmentClient;

    private StoreUnit seedUnit(StoreUnitStatus status) {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 1, OffsetDateTime.now()));
        return storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country", status,
                OffsetDateTime.now()));
    }

    private void setLeaseToken(UUID unitId, UUID token) {
        // Test-only: lease_token is otherwise written exclusively by StoreUnitClaimDao's native
        // claim query, never through the entity, so there's no repository setter for it.
        jdbcTemplate.update("UPDATE store_units SET lease_token = ? WHERE id = ?", token, unitId);
    }

    @Test
    void successWritesSucceededAndEnrichmentResult() {
        StoreUnit unit = seedUnit(StoreUnitStatus.IN_PROGRESS);
        UUID leaseToken = UUID.randomUUID();
        setLeaseToken(unit.getId(), leaseToken);
        when(enrichmentClient.enrich(any())).thenReturn(new EnrichResult(18234, 142033.50, 6210));

        worker.processClaimedUnit(new ClaimedUnit(unit.getId(), leaseToken, "ST1", "A", "Addr", "City", "State"));

        StoreUnit updated = storeUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(StoreUnitStatus.SUCCEEDED);
        assertThat(enrichmentResultRepository.findByStoreUnitId(unit.getId())).isPresent();
        assertThat(enrichmentResultRepository.findByStoreUnitId(unit.getId()).get().getEstimatedMonthlyFootfall())
                .isEqualTo(18234);
    }

    @Test
    void failureRequeuesWhenAttemptsRemain() {
        StoreUnit unit = seedUnit(StoreUnitStatus.IN_PROGRESS); // attempt_count starts at 0
        UUID leaseToken = UUID.randomUUID();
        setLeaseToken(unit.getId(), leaseToken);
        when(enrichmentClient.enrich(any())).thenThrow(new EnrichmentCallException("Upstream 500: boom"));

        worker.processClaimedUnit(new ClaimedUnit(unit.getId(), leaseToken, "ST1", "A", "Addr", "City", "State"));

        StoreUnit updated = storeUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(StoreUnitStatus.PENDING);
        assertThat(updated.getLastError()).contains("500");
    }

    @Test
    void staleLeaseTokenIsANoOp() {
        StoreUnit unit = seedUnit(StoreUnitStatus.SUCCEEDED); // a newer attempt already succeeded
        UUID currentToken = UUID.randomUUID();
        UUID staleToken = UUID.randomUUID();
        setLeaseToken(unit.getId(), currentToken);
        when(enrichmentClient.enrich(any())).thenReturn(new EnrichResult(1, 1.0, 1));

        // A late response for the OLD (already-superseded) attempt tries to write with staleToken.
        worker.processClaimedUnit(new ClaimedUnit(unit.getId(), staleToken, "ST1", "A", "Addr", "City", "State"));

        StoreUnit unchanged = storeUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(StoreUnitStatus.SUCCEEDED);
        assertThat(enrichmentResultRepository.findByStoreUnitId(unit.getId())).isEmpty();
    }

    @Test
    void failureSetsBackoffThatDelaysReclaim() throws InterruptedException {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "stores.csv", 1, OffsetDateTime.now()));
        StoreUnit unit = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country",
                StoreUnitStatus.IN_PROGRESS, OffsetDateTime.now()));
        UUID leaseToken = UUID.randomUUID();
        setLeaseToken(unit.getId(), leaseToken);
        when(enrichmentClient.enrich(any())).thenThrow(new EnrichmentCallException("Upstream 500: boom"));

        worker.processClaimedUnit(new ClaimedUnit(unit.getId(), leaseToken, "ST1", "A", "Addr", "City", "State"));

        // Immediately after failing, the unit is PENDING but its backoff hasn't elapsed yet.
        List<ClaimedUnit> immediateClaim = claimDao.claimBatch(job.getId(), 60, 5, 10);
        assertThat(immediateClaim).isEmpty();

        // Test profile's retry-backoff-max is 50ms - wait past it, then it's claimable again.
        Thread.sleep(100);
        List<ClaimedUnit> laterClaim = claimDao.claimBatch(job.getId(), 60, 5, 10);
        assertThat(laterClaim).extracting(ClaimedUnit::id).containsExactly(unit.getId());
    }

    @Test
    void unexpectedExceptionIsCaughtAndRequeues() {
        StoreUnit unit = seedUnit(StoreUnitStatus.IN_PROGRESS);
        UUID leaseToken = UUID.randomUUID();
        setLeaseToken(unit.getId(), leaseToken);
        when(enrichmentClient.enrich(any())).thenThrow(new RuntimeException("something unrelated broke"));

        worker.processClaimedUnit(new ClaimedUnit(unit.getId(), leaseToken, "ST1", "A", "Addr", "City", "State"));

        StoreUnit updated = storeUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(StoreUnitStatus.PENDING);
        assertThat(updated.getLastError()).contains("Unexpected error");
    }
}
