package com.tierforge.app.monitor;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MonitorControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogBuffer logBuffer;

    @Test
    void listsJobsWithCountsRetryingAndPercentDone() throws Exception {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "monitor.csv", 4, OffsetDateTime.now()));
        unit(job, "M1", StoreUnitStatus.SUCCEEDED);
        unit(job, "M2", StoreUnitStatus.FAILED);
        StoreUnit retrying = unit(job, "M3", StoreUnitStatus.PENDING);
        StoreUnit inFlight = unit(job, "M4", StoreUnitStatus.IN_PROGRESS);
        jdbcTemplate.update("UPDATE store_units SET claimed_at = now() WHERE id = ?", inFlight.getId());
        jdbcTemplate.update(
                "UPDATE store_units SET attempt_count = 1, last_error = 'Upstream 500' WHERE id = ?",
                retrying.getId());

        String row = "$[?(@.id == '" + job.getId() + "')]";
        mockMvc.perform(get("/api/monitor/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row + ".status", hasItem("RUNNING")))
                .andExpect(jsonPath(row + ".succeeded", hasItem(1)))
                .andExpect(jsonPath(row + ".failed", hasItem(1)))
                .andExpect(jsonPath(row + ".pending", hasItem(1)))
                .andExpect(jsonPath(row + ".inProgress", hasItem(1)))
                .andExpect(jsonPath(row + ".retrying", hasItem(1)))
                .andExpect(jsonPath(row + ".percentDone", hasItem(50)))
                .andExpect(jsonPath(row + ".lastActivityAt", hasItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void reportsErrorReasonsAndRecentErrorsForAJob() throws Exception {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.RUNNING, "errors.csv", 2, OffsetDateTime.now()));
        StoreUnit a = unit(job, "E1", StoreUnitStatus.FAILED);
        StoreUnit b = unit(job, "E2", StoreUnitStatus.PENDING);
        jdbcTemplate.update(
                "UPDATE store_units SET attempt_count = 5, last_error = 'Upstream 429: rate limited' WHERE id = ?",
                a.getId());
        jdbcTemplate.update(
                "UPDATE store_units SET attempt_count = 1, last_error = 'Upstream 429: rate limited' WHERE id = ?",
                b.getId());

        jdbcTemplate.update("UPDATE store_units SET claimed_at = now() - interval '5 minutes' WHERE id = ?", a.getId());
        jdbcTemplate.update("UPDATE store_units SET claimed_at = now() WHERE id = ?", b.getId());

        mockMvc.perform(get("/api/monitor/errors").param("jobId", job.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recent[0].storeId", is("E2")))
                .andExpect(jsonPath("$.recent[1].storeId", is("E1")))
                .andExpect(jsonPath("$.recent[0].lastAttemptAt", org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.reasons[0].reason", is("Upstream 429: rate limited")))
                .andExpect(jsonPath("$.reasons[0].count", is(2)))
                .andExpect(jsonPath("$.recent.length()", is(2)))
                .andExpect(jsonPath("$.recent[*].storeId", hasItem("E1")));
    }

    @Test
    void servesCapturedLogsIncrementallyAndResetsStaleCursor() throws Exception {
        org.slf4j.LoggerFactory.getLogger("com.tierforge.app.enrichment.MonitorTestLogger")
                .info("monitor-test-marker");

        mockMvc.perform(get("/api/monitor/logs").param("after", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].message", hasItem("monitor-test-marker")))
                .andExpect(jsonPath("$.lastSeq", greaterThanOrEqualTo(1)));

        long last = logBuffer.lastSeq();
        mockMvc.perform(get("/api/monitor/logs").param("after", String.valueOf(last)))
                .andExpect(jsonPath("$.entries.length()", is(0)))
                .andExpect(jsonPath("$.lastSeq", is((int) last)));

        // Cursor ahead of the buffer (as after a backend restart) starts over instead of hanging.
        mockMvc.perform(get("/api/monitor/logs").param("after", String.valueOf(last + 1_000_000)))
                .andExpect(jsonPath("$.entries[*].message", hasItem("monitor-test-marker")));
    }

    private StoreUnit unit(Job job, String storeId, StoreUnitStatus status) {
        return storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, storeId, "A", "Addr", "City", "State", "Country",
                status, OffsetDateTime.now()));
    }
}
