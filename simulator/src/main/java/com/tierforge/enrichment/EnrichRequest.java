package com.tierforge.enrichment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnrichRequest(
        @JsonProperty("store_id") String storeId,
        @JsonProperty("store_name") String storeName,
        @JsonProperty("address") String address,
        @JsonProperty("city") String city,
        @JsonProperty("state") String state) {
}
