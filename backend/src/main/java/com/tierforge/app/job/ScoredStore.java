package com.tierforge.app.job;

public record ScoredStore(
        String storeId,
        String storeName,
        int footfall,
        double revenue,
        int sqft,
        int score,
        StoreTier tier) {
}
