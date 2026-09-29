package com.tierforge.app.job;

import java.util.UUID;

public record ClaimedUnit(
        UUID id, UUID leaseToken, String storeId, String storeName, String address, String city, String state) {
}
