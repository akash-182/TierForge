package com.tierforge.app.web;

import com.tierforge.app.scoring.ScoringValidationException;
import com.tierforge.app.upload.CsvValidationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(CsvValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCsvValidation(CsvValidationException ex) {
        log.warn("CSV upload rejected: {}", ex.getErrors());
        return ResponseEntity.badRequest().body(Map.of("errors", ex.getErrors()));
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJobNotFound(JobNotFoundException ex) {
        log.warn("Job not found: {}", ex.getJobId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Job not found: " + ex.getJobId()));
    }

    @ExceptionHandler(JobNotStartableException.class)
    public ResponseEntity<Map<String, String>> handleJobNotStartable(JobNotStartableException ex) {
        log.warn("Job not startable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ScoringValidationException.class)
    public ResponseEntity<Map<String, String>> handleScoringValidation(ScoringValidationException ex) {
        log.warn("Scoring config rejected: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ScoringNotConfiguredException.class)
    public ResponseEntity<Map<String, String>> handleScoringNotConfigured(ScoringNotConfiguredException ex) {
        log.warn("Scoring not configured: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
