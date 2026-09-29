package com.tierforge.app.web;

import com.tierforge.app.job.Job;
import com.tierforge.app.job.JobRepository;
import com.tierforge.app.upload.CsvValidationException;
import com.tierforge.app.upload.JobUploadService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobUploadService jobUploadService;
    private final JobRepository jobRepository;

    public JobController(JobUploadService jobUploadService, JobRepository jobRepository) {
        this.jobUploadService = jobUploadService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobResponse> uploadJob(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new CsvValidationException(List.of("Uploaded file is empty"));
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv";
        if (!filename.toLowerCase().endsWith(".csv")) {
            throw new CsvValidationException(List.of("Only .csv files are supported"));
        }

        Job job = jobUploadService.createJobFromCsv(filename, file.getInputStream());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        return ResponseEntity.ok(JobResponse.from(job));
    }
}
