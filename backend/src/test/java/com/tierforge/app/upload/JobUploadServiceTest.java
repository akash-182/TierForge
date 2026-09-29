package com.tierforge.app.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.tierforge.app.AbstractIntegrationTest;
import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnitRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JobUploadServiceTest extends AbstractIntegrationTest {

    @Autowired
    private JobUploadService jobUploadService;

    @Autowired
    private StoreUnitRepository storeUnitRepository;

    @Test
    void createsJobAndStoreUnitsFromValidCsv() throws IOException {
        String csv = "store_id,store_name,address,city,state,country\n"
                + "ST000001,Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n"
                + "ST000002,City Pharmacy #2,27 Hill Street,New Delhi,Delhi,India\n"
                + "ST000003,Neighborhood Supermarket #3,9 Main Road,Mumbai,Maharashtra,India\n";

        Job job = jobUploadService.createJobFromCsv(
                "stores.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(job.getId()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getSourceFilename()).isEqualTo("stores.csv");
        assertThat(job.getTotalStoreCount()).isEqualTo(3);
        assertThat(storeUnitRepository.countByJobId(job.getId())).isEqualTo(3);
    }

    @Test
    void rejectsInvalidCsvWithoutCreatingAJob() {
        String csv = "store_id,store_name,address,city,state,country\n"
                + ",Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n";

        org.junit.jupiter.api.Assertions.assertThrows(
                CsvValidationException.class,
                () -> jobUploadService.createJobFromCsv(
                        "bad.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
    }
}
