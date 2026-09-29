package com.tierforge.app.web;

import com.tierforge.app.upload.CsvValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CsvValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCsvValidation(CsvValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("errors", ex.getErrors()));
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJobNotFound(JobNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Job not found: " + ex.getJobId()));
    }
}
