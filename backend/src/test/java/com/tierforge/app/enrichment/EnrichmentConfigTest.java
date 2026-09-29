package com.tierforge.app.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// No @ActiveProfiles("test") and doesn't extend AbstractIntegrationTest: this verifies the
// checked-in application.properties defaults, not the fast test-profile overrides, and needs
// no database at all (classes = EnrichmentConfig.class keeps autoconfiguration — and any
// datasource/Flyway machinery — out of this context entirely).
@SpringBootTest(classes = EnrichmentConfig.class)
class EnrichmentConfigTest {

    @Autowired
    private EnrichmentProperties properties;

    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void bindsDefaultProperties() {
        assertThat(properties.baseUrl()).isEqualTo("http://localhost:8000");
        assertThat(properties.rateLimitPerSecond()).isEqualTo(5);
        assertThat(properties.clientTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.maxAttempts()).isEqualTo(5);
        assertThat(properties.leaseDuration()).isEqualTo(Duration.ofSeconds(12));
    }

    @Test
    void rateLimiterReflectsConfiguredLimit() {
        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(5);
    }
}
