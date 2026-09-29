package com.tierforge.app.web;

import com.tierforge.app.job.StoreUnit;

public record StoreUnitResponse(String storeId, String status, int attemptCount, String lastError) {

    public static StoreUnitResponse from(StoreUnit unit) {
        return new StoreUnitResponse(
                unit.getStoreId(), unit.getStatus().name(), unit.getAttemptCount(), unit.getLastError());
    }
}
