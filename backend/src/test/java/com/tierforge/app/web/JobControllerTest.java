package com.tierforge.app.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tierforge.app.AbstractIntegrationTest;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class JobControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadsFullSampleFileAndCreatesJob() throws Exception {
        byte[] csvBytes;
        try (InputStream in = new ClassPathResource("stores_5000.csv").getInputStream()) {
            csvBytes = in.readAllBytes();
        }
        MockMultipartFile file = new MockMultipartFile("file", "stores_5000.csv", "text/csv", csvBytes);

        String responseJson = mockMvc.perform(multipart("/api/jobs").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.sourceFilename", is("stores_5000.csv")))
                .andExpect(jsonPath("$.totalStoreCount", is(5000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = com.jayway.jsonpath.JsonPath.read(responseJson, "$.id");

        mockMvc.perform(get("/api/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStoreCount", is(5000)));
    }

    @Test
    void rejectsUploadWithBadHeaderAndListsErrors() throws Exception {
        String csv = "store_id,store_name,address,city\nST000001,X,Y,Z\n";
        MockMultipartFile file =
                new MockMultipartFile("file", "bad.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/jobs").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.errors[0]").exists());
    }

    @Test
    void returnsNotFoundForUnknownJob() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
