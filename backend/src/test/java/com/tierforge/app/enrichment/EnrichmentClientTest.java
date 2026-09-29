package com.tierforge.app.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(EnrichmentClient.class)
@EnableConfigurationProperties(EnrichmentProperties.class)
class EnrichmentClientTest {

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private EnrichmentClient client;

    @Test
    void mapsSuccessfulResponse() {
        server.expect(requestTo("http://localhost:8000/enrich"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"store_id\":\"ST000001\",\"estimated_monthly_footfall\":18234,"
                                + "\"estimated_monthly_revenue\":142033.50,\"store_size_sqft\":6210}",
                        MediaType.APPLICATION_JSON));

        EnrichResult result = client.enrich(new EnrichRequest("ST000001", "Store", "Addr", "City", "State"));

        assertThat(result.footfall()).isEqualTo(18234);
        assertThat(result.revenue()).isEqualTo(142033.50);
        assertThat(result.sqft()).isEqualTo(6210);
    }

    @Test
    void mapsServerErrorToEnrichmentCallException() {
        server.expect(requestTo("http://localhost:8000/enrich")).andRespond(withServerError());

        assertThatThrownBy(() -> client.enrich(new EnrichRequest("ST000001", "Store", "Addr", "City", "State")))
                .isInstanceOf(EnrichmentCallException.class)
                .hasMessageContaining("500");
    }

    @Test
    void mapsTooManyRequestsToEnrichmentCallException() {
        server.expect(requestTo("http://localhost:8000/enrich"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.enrich(new EnrichRequest("ST000001", "Store", "Addr", "City", "State")))
                .isInstanceOf(EnrichmentCallException.class)
                .hasMessageContaining("429");
    }
}
