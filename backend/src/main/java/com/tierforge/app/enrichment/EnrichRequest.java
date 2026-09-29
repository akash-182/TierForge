package com.tierforge.app.enrichment;

public record EnrichRequest(String storeId, String storeName, String address, String city, String state) {
}
