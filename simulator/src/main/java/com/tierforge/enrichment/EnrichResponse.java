package com.tierforge.enrichment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnrichResponse(
        @JsonProperty("store_id") String storeId,
        @JsonProperty("estimated_monthly_footfall") int estimatedMonthlyFootfall,
        @JsonProperty("estimated_monthly_revenue") double estimatedMonthlyRevenue,
        @JsonProperty("store_size_sqft") int storeSizeSqft) {
}
