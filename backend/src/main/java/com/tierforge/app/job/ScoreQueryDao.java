package com.tierforge.app.job;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Joins three tables (store_scores, store_units, enrichment_results) for the dashboard's store
// list, which JPA can't express cleanly since EnrichmentResult/StoreScore are plain FK columns
// rather than mapped associations on StoreUnit (deliberately, to keep those writes simple).
@Repository
public class ScoreQueryDao {

    private final JdbcTemplate jdbcTemplate;

    public ScoreQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ScoredStore> listScores(UUID jobId, StoreTier tierFilter) {
        String sql = """
                SELECT su.store_id, su.store_name,
                       er.estimated_monthly_footfall, er.estimated_monthly_revenue, er.store_size_sqft,
                       ss.score, ss.tier
                FROM store_scores ss
                JOIN store_units su ON su.id = ss.store_unit_id
                JOIN enrichment_results er ON er.store_unit_id = ss.store_unit_id
                WHERE ss.job_id = ?
                """
                + (tierFilter != null ? "  AND ss.tier = ?\n" : "")
                + "ORDER BY su.store_id";

        Object[] params = tierFilter != null
                ? new Object[] {jobId, tierFilter.name()}
                : new Object[] {jobId};

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ScoredStore(
                        rs.getString("store_id"),
                        rs.getString("store_name"),
                        rs.getInt("estimated_monthly_footfall"),
                        rs.getDouble("estimated_monthly_revenue"),
                        rs.getInt("store_size_sqft"),
                        rs.getInt("score"),
                        StoreTier.valueOf(rs.getString("tier"))),
                params);
    }
}
