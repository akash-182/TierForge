package com.tierforge.app.web;

import com.tierforge.app.job.ScoredStore;

public record ScoredStoreResponse(
        String storeId, String storeName, int footfall, double revenue, int sqft, int score, String tier) {

    public static ScoredStoreResponse from(ScoredStore store) {
        return new ScoredStoreResponse(
                store.storeId(), store.storeName(), store.footfall(), store.revenue(), store.sqft(),
                store.score(), store.tier().name());
    }
}
