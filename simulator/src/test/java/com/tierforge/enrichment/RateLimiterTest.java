package com.tierforge.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RateLimiterTest {

    @Test
    void allowsUpToFiveRequestsPerSecondThenRejects() {
        RateLimiter rateLimiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            rateLimiter.checkLimit();
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, rateLimiter::checkLimit);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }
}
