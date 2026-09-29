package com.tierforge.enrichment;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Produces metrics that are deterministic per store_id: hashing the id into a seed
 * means the same store always gets the same numbers from this running instance.
 */
final class MetricsGenerator {

    private static final BigInteger SEED_MODULUS = BigInteger.ONE.shiftLeft(32);

    private MetricsGenerator() {
    }

    static EnrichResponse deterministicMetrics(String storeId) {
        Random rng = new Random(seedFor(storeId));

        int footfall = 500 + rng.nextInt(50_000 - 500 + 1);
        double revenue = round2(5_000 + (500_000 - 5_000) * rng.nextDouble());
        int sqft = 200 + rng.nextInt(20_000 - 200 + 1);

        return new EnrichResponse(storeId, footfall, revenue, sqft);
    }

    private static long seedFor(String storeId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(storeId.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, hash).mod(SEED_MODULUS).longValueExact();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
