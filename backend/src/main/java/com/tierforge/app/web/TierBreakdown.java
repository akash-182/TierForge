package com.tierforge.app.web;

import com.tierforge.app.job.StoreScoreRepository;
import java.util.List;

public record TierBreakdown(long large, long medium, long small) {

    public static TierBreakdown from(List<StoreScoreRepository.TierCount> rows) {
        long large = 0;
        long medium = 0;
        long small = 0;
        for (StoreScoreRepository.TierCount row : rows) {
            switch (row.getTier()) {
                case LARGE -> large = row.getCount();
                case MEDIUM -> medium = row.getCount();
                case SMALL -> small = row.getCount();
            }
        }
        return new TierBreakdown(large, medium, small);
    }
}
