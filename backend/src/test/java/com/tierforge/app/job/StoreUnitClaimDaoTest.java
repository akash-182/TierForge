package com.tierforge.app.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.tierforge.app.AbstractIntegrationTest;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StoreUnitClaimDaoTest extends AbstractIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private StoreUnitClaimDao claimDao;

    @Test
    void claimsOnlyEligibleUnits() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 2, OffsetDateTime.now()));
        StoreUnit pending = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));
        StoreUnit succeeded = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST2", "B", "Addr", "City", "State", "Country",
                StoreUnitStatus.SUCCEEDED, OffsetDateTime.now()));

        List<ClaimedUnit> claimed = claimDao.claimBatch(job.getId(), 60, 5, 100);

        assertThat(claimed).extracting(ClaimedUnit::id).containsExactly(pending.getId());
        assertThat(storeUnitRepository.findById(succeeded.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreUnitStatus.SUCCEEDED);
    }

    @Test
    void concurrentClaimsNeverOverlap() throws Exception {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 50, OffsetDateTime.now()));
        for (int i = 0; i < 50; i++) {
            storeUnitRepository.save(new StoreUnit(
                    UUID.randomUUID(), job, "ST" + i, "Store " + i, "Addr", "City", "State", "Country",
                    StoreUnitStatus.PENDING, OffsetDateTime.now()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CompletableFuture<List<ClaimedUnit>> first =
                CompletableFuture.supplyAsync(() -> claimDao.claimBatch(job.getId(), 60, 5, 100), executor);
        CompletableFuture<List<ClaimedUnit>> second =
                CompletableFuture.supplyAsync(() -> claimDao.claimBatch(job.getId(), 60, 5, 100), executor);
        CompletableFuture.allOf(first, second).join();
        executor.shutdown();

        Set<UUID> firstIds = first.get().stream().map(ClaimedUnit::id).collect(Collectors.toSet());
        Set<UUID> secondIds = second.get().stream().map(ClaimedUnit::id).collect(Collectors.toSet());

        // Two concurrent single-statement claims often split as "one gets everything, the other
        // gets nothing" rather than an even split — that's still correct (each row was claimed
        // exactly once), so check the intersection is empty rather than requiring both non-empty.
        Set<UUID> intersection = new HashSet<>(firstIds);
        intersection.retainAll(secondIds);
        assertThat(intersection).isEmpty();

        Set<UUID> combined = new HashSet<>(firstIds);
        combined.addAll(secondIds);
        assertThat(combined).hasSize(50);
    }

    @Test
    void doesNotClaimUnitsAtMaxAttempts() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 1, OffsetDateTime.now()));
        storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));

        claimDao.claimBatch(job.getId(), 0, 2, 100); // attempt 1, immediately-expired lease
        List<ClaimedUnit> secondClaim = claimDao.claimBatch(job.getId(), 60, 2, 100); // attempt 2
        assertThat(secondClaim).hasSize(1);

        List<ClaimedUnit> thirdClaim = claimDao.claimBatch(job.getId(), 60, 2, 100); // attempt_count now 2, >= max
        assertThat(thirdClaim).isEmpty();
    }

    @Test
    void capsClaimsAtMaxToClaim() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 10, OffsetDateTime.now()));
        for (int i = 0; i < 10; i++) {
            storeUnitRepository.save(new StoreUnit(
                    UUID.randomUUID(), job, "ST" + i, "Store " + i, "Addr", "City", "State", "Country",
                    StoreUnitStatus.PENDING, OffsetDateTime.now()));
        }

        List<ClaimedUnit> claimed = claimDao.claimBatch(job.getId(), 60, 5, 3);

        assertThat(claimed).hasSize(3);
        assertThat(storeUnitRepository.countByJobIdAndStatusIn(job.getId(), List.of(StoreUnitStatus.PENDING)))
                .isEqualTo(7);
    }

    @Test
    void markSucceededIsNoOpForStaleLeaseToken() {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, "stores.csv", 1, OffsetDateTime.now()));
        StoreUnit unit = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST1", "A", "Addr", "City", "State", "Country",
                StoreUnitStatus.PENDING, OffsetDateTime.now()));
        List<ClaimedUnit> claimed = claimDao.claimBatch(job.getId(), 60, 5, 100);
        UUID realToken = claimed.get(0).leaseToken();
        UUID staleToken = UUID.randomUUID();

        int updated = storeUnitRepository.markSucceeded(unit.getId(), staleToken);

        assertThat(updated).isZero();
        assertThat(storeUnitRepository.findById(unit.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreUnitStatus.IN_PROGRESS);
        assertThat(realToken).isNotEqualTo(staleToken);
    }
}
