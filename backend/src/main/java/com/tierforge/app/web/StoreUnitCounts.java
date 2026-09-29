package com.tierforge.app.web;

import com.tierforge.app.job.StoreUnitRepository;
import java.util.List;

public record StoreUnitCounts(long pending, long inProgress, long succeeded, long failed) {

    public static StoreUnitCounts from(List<StoreUnitRepository.StatusCount> rows) {
        long pending = 0;
        long inProgress = 0;
        long succeeded = 0;
        long failed = 0;
        for (StoreUnitRepository.StatusCount row : rows) {
            switch (row.getStatus()) {
                case PENDING -> pending = row.getCount();
                case IN_PROGRESS -> inProgress = row.getCount();
                case SUCCEEDED -> succeeded = row.getCount();
                case FAILED -> failed = row.getCount();
            }
        }
        return new StoreUnitCounts(pending, inProgress, succeeded, failed);
    }
}
