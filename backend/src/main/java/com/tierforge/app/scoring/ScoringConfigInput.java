package com.tierforge.app.scoring;

public record ScoringConfigInput(
        int footfallBar,
        int footfallWeight,
        double revenueBar,
        int revenueWeight,
        int sizeBar,
        int sizeWeight,
        int tierLargeThreshold,
        int tierMediumThreshold) {
}
