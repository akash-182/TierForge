package com.tierforge.app.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.EnrichmentResult;
import com.tierforge.app.job.EnrichmentResultRepository;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ScoringControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Autowired
    private EnrichmentResultRepository enrichmentResultRepository;

    private UUID seedJobWithOneSucceededStore(int footfall, double revenue, int sqft) {
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.COMPLETED, "stores.csv", 1, OffsetDateTime.now()));
        StoreUnit unit = storeUnitRepository.save(new StoreUnit(
                UUID.randomUUID(), job, "ST000001", "Fresh Supermarket", "Addr", "City", "State", "Country",
                StoreUnitStatus.SUCCEEDED, OffsetDateTime.now()));
        enrichmentResultRepository.save(new EnrichmentResult(
                UUID.randomUUID(), unit.getId(), footfall, revenue, sqft, OffsetDateTime.now()));
        return job.getId();
    }

    private Map<String, Object> validConfigBody() {
        return Map.of(
                "footfallBar", 15000, "footfallWeight", 50,
                "revenueBar", 150000.0, "revenueWeight", 30,
                "sizeBar", 8000, "sizeWeight", 20,
                "tierLargeThreshold", 70, "tierMediumThreshold", 40);
    }

    @Test
    void submitScoringComputesAndReturnsSummary() throws Exception {
        UUID jobId = seedJobWithOneSucceededStore(40_000, 400_000, 18_000);

        mockMvc.perform(post("/api/jobs/{id}/scoring", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validConfigBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.footfallBar", is(15000)))
                .andExpect(jsonPath("$.tierBreakdown.large", is(1)))
                .andExpect(jsonPath("$.scoredStoreCount", is(1)));
    }

    @Test
    void submitScoringWithBadWeightsReturnsBadRequest() throws Exception {
        UUID jobId = seedJobWithOneSucceededStore(40_000, 400_000, 18_000);
        Map<String, Object> badBody = Map.of(
                "footfallBar", 15000, "footfallWeight", 60,
                "revenueBar", 150000.0, "revenueWeight", 30,
                "sizeBar", 8000, "sizeWeight", 20,
                "tierLargeThreshold", 70, "tierMediumThreshold", 40);

        mockMvc.perform(post("/api/jobs/{id}/scoring", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getScoringBeforeSubmissionReturnsNotFound() throws Exception {
        UUID jobId = seedJobWithOneSucceededStore(40_000, 400_000, 18_000);

        mockMvc.perform(get("/api/jobs/{id}/scoring", jobId)).andExpect(status().isNotFound());
    }

    @Test
    void listScoresFiltersByTier() throws Exception {
        UUID jobId = seedJobWithOneSucceededStore(40_000, 400_000, 18_000);
        mockMvc.perform(post("/api/jobs/{id}/scoring", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validConfigBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/{id}/scores", jobId).param("tier", "LARGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeId", is("ST000001")))
                .andExpect(jsonPath("$[0].tier", is("LARGE")));

        mockMvc.perform(get("/api/jobs/{id}/scores", jobId).param("tier", "SMALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void scoringEndpointsReturnNotFoundForUnknownJob() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(get("/api/jobs/{id}/scores", unknownId)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/jobs/{id}/scoring", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validConfigBody())))
                .andExpect(status().isNotFound());
    }
}
