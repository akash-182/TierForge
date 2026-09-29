package com.tierforge.app.web;

public record ScoringSummaryResponse(
        ScoringConfigResponse config, TierBreakdown tierBreakdown, int scoredStoreCount) {
}
