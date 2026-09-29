package com.tierforge.enrichment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RateLimiter {

    private static final int LIMIT_PER_SECOND = 5;
    private static final long WINDOW_NANOS = 1_000_000_000L;

    private long windowStart = System.nanoTime();
    private int windowCount = 0;

    public synchronized void checkLimit() {
        long now = System.nanoTime();
        if (now - windowStart >= WINDOW_NANOS) {
            windowStart = now;
            windowCount = 0;
        }
        windowCount++;
        if (windowCount > LIMIT_PER_SECOND) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded: 5 requests/second");
        }
    }
}
