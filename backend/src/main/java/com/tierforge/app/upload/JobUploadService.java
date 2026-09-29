package com.tierforge.app.upload;

import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.job.JobStatus;
import com.tierforge.app.job.StoreUnit;
import com.tierforge.app.job.StoreUnitRepository;
import com.tierforge.app.job.StoreUnitStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobUploadService {

    private final JobRepository jobRepository;
    private final StoreUnitRepository storeUnitRepository;

    public JobUploadService(JobRepository jobRepository, StoreUnitRepository storeUnitRepository) {
        this.jobRepository = jobRepository;
        this.storeUnitRepository = storeUnitRepository;
    }

    @Transactional
    public Job createJobFromCsv(String filename, InputStream csvInputStream) throws IOException {
        ParsedCsv parsed;
        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            parsed = CsvJobParser.parse(reader);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Job job = jobRepository.save(
                new Job(UUID.randomUUID(), JobStatus.PENDING, filename, parsed.rows().size(), now));

        List<StoreUnit> units = new ArrayList<>();
        for (CsvStoreRow row : parsed.rows()) {
            units.add(new StoreUnit(
                    UUID.randomUUID(),
                    job,
                    row.storeId(),
                    row.storeName(),
                    row.address(),
                    row.city(),
                    row.state(),
                    row.country(),
                    StoreUnitStatus.PENDING,
                    now));
        }
        storeUnitRepository.saveAll(units);

        return job;
    }
}
