package com.tierforge.enrichment;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class EnrichController {

    private static final double HANG_RATE = 0.02;
    private static final double ERROR_RATE = 0.10;
    private static final long HANG_SECONDS = 50;
    private static final double NORMAL_DELAY_MIN_SECONDS = 0.2;
    private static final double NORMAL_DELAY_MAX_SECONDS = 0.5;

    private final RateLimiter rateLimiter;

    public EnrichController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/enrich")
    public EnrichResponse enrich(@RequestBody EnrichRequest request) {
        rateLimiter.checkLimit();

        double roll = Math.random();

        if (roll < HANG_RATE) {
            sleepSeconds(HANG_SECONDS);
            return MetricsGenerator.deterministicMetrics(request.storeId());
        }

        if (roll < HANG_RATE + ERROR_RATE) {
            sleepSeconds(randomNormalDelay());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transient upstream error");
        }

        sleepSeconds(randomNormalDelay());
        return MetricsGenerator.deterministicMetrics(request.storeId());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private static double randomNormalDelay() {
        return NORMAL_DELAY_MIN_SECONDS
                + (NORMAL_DELAY_MAX_SECONDS - NORMAL_DELAY_MIN_SECONDS) * Math.random();
    }

    private static void sleepSeconds(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
