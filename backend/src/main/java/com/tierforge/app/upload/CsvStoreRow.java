package com.tierforge.app.upload;

public record CsvStoreRow(
        String storeId, String storeName, String address, String city, String state, String country) {
}
