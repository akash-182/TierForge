package com.tierforge.app.enrichment;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(EnrichmentProperties.class)
public class EnrichmentConfig {

    @Bean
    public RateLimiter enrichmentRateLimiter(EnrichmentProperties properties) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.rateLimitPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                // Generous: a large job can take well over an hour to drain through the rate
                // limiter — workers queue here, not in a hand-sized thread pool.
                .timeoutDuration(Duration.ofHours(1))
                .build();
        return RateLimiter.of("enrichment-api", config);
    }

    // Applied automatically by Spring Boot to every auto-configured RestClient.Builder,
    // including inside @RestClientTest slices (after MockRestServiceServer's own binding, so
    // it never fights that test infrastructure for control of the request factory).
    @Bean
    public RestClientCustomizer enrichmentTimeoutCustomizer(EnrichmentProperties properties) {
        return builder -> {
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(properties.clientTimeout())
                    .withReadTimeout(properties.clientTimeout());
            ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
            builder.requestFactory(factory);
        };
    }
}
