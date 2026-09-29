package com.tierforge.app.enrichment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tierforge.enrichment")
public record EnrichmentProperties(
        @DefaultValue("http://localhost:8000") String baseUrl,
        @DefaultValue("5") int rateLimitPerSecond,
        @DefaultValue("10s") Duration clientTimeout,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("12s") Duration leaseDuration,
        @DefaultValue("1s") Duration orchestratorPollInterval) {
}
