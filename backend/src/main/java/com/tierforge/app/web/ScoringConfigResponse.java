package com.tierforge.app.web;

import com.tierforge.app.job.ScoringConfig;
import java.time.OffsetDateTime;

public record ScoringConfigResponse(
        int footfallBar,
        int footfallWeight,
        double revenueBar,
        int revenueWeight,
        int sizeBar,
        int sizeWeight,
        int tierLargeThreshold,
        int tierMediumThreshold,
        OffsetDateTime updatedAt) {

    public static ScoringConfigResponse from(ScoringConfig config) {
        return new ScoringConfigResponse(
                config.getFootfallBar(), config.getFootfallWeight(), config.getRevenueBar(),
                config.getRevenueWeight(), config.getSizeBar(), config.getSizeWeight(),
                config.getTierLargeThreshold(), config.getTierMediumThreshold(), config.getUpdatedAt());
    }
}
