package com.tierforge.app.enrichment;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class EnrichmentClient {

    private final RestClient restClient;

    public EnrichmentClient(RestClient.Builder restClientBuilder, EnrichmentProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    public EnrichResult enrich(EnrichRequest request) {
        try {
            SimulatorEnrichResponse response = restClient
                    .post()
                    .uri("/enrich")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SimulatorEnrichRequest(
                            request.storeId(), request.storeName(), request.address(), request.city(),
                            request.state()))
                    .retrieve()
                    .body(SimulatorEnrichResponse.class);
            return new EnrichResult(
                    response.estimatedMonthlyFootfall(), response.estimatedMonthlyRevenue(),
                    response.storeSizeSqft());
        } catch (HttpServerErrorException e) {
            throw new EnrichmentCallException("Upstream 500: " + e.getMessage());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new EnrichmentCallException("Upstream 429: rate limited");
        } catch (ResourceAccessException e) {
            throw new EnrichmentCallException("Timed out waiting for response");
        }
    }

    record SimulatorEnrichRequest(
            @JsonProperty("store_id") String storeId,
            @JsonProperty("store_name") String storeName,
            @JsonProperty("address") String address,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state) {
    }

    record SimulatorEnrichResponse(
            @JsonProperty("store_id") String storeId,
            @JsonProperty("estimated_monthly_footfall") int estimatedMonthlyFootfall,
            @JsonProperty("estimated_monthly_revenue") double estimatedMonthlyRevenue,
            @JsonProperty("store_size_sqft") int storeSizeSqft) {
    }
}
