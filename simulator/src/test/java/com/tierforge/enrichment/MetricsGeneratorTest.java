package com.tierforge.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MetricsGeneratorTest {

    @Test
    void sameStoreIdProducesSameMetrics() {
        EnrichResponse first = MetricsGenerator.deterministicMetrics("ST000001");
        EnrichResponse second = MetricsGenerator.deterministicMetrics("ST000001");

        assertEquals(first, second);
    }

    @Test
    void metricsFallWithinDocumentedRanges() {
        EnrichResponse response = MetricsGenerator.deterministicMetrics("ST000042");

        assertTrue(response.estimatedMonthlyFootfall() >= 500 && response.estimatedMonthlyFootfall() <= 50_000);
        assertTrue(response.estimatedMonthlyRevenue() >= 5_000 && response.estimatedMonthlyRevenue() <= 500_000);
        assertTrue(response.storeSizeSqft() >= 200 && response.storeSizeSqft() <= 20_000);
    }

    @Test
    void differentStoreIdsProduceDifferentMetrics() {
        EnrichResponse a = MetricsGenerator.deterministicMetrics("ST000001");
        EnrichResponse b = MetricsGenerator.deterministicMetrics("ST000002");

        assertTrue(!a.equals(b));
    }
}
