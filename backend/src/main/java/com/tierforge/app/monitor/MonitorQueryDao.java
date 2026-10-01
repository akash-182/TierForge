package com.tierforge.app.monitor;

import com.tierforge.app.monitor.MonitorDtos.ErrorReason;
import com.tierforge.app.monitor.MonitorDtos.JobMonitorRow;
import com.tierforge.app.monitor.MonitorDtos.RecentError;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Read-only aggregate queries for the live monitor. Kept separate from the job repositories so the
// monitor stays an independent add-on; grouped queries avoid per-job N+1 lookups.
@Repository
public class MonitorQueryDao {

    private static final int THROUGHPUT_WINDOW_SECONDS = 60;

    private final JdbcTemplate jdbcTemplate;

    public MonitorQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<JobMonitorRow> listJobs() {
        String sql = """
                SELECT j.id, j.status, j.source_filename, j.total_store_count, j.created_at,
                       MAX(su.claimed_at) AS last_activity_at,
                       COUNT(su.id) FILTER (WHERE su.status = 'PENDING') AS pending,
                       COUNT(su.id) FILTER (WHERE su.status = 'IN_PROGRESS') AS in_progress,
                       COUNT(su.id) FILTER (WHERE su.status = 'SUCCEEDED') AS succeeded,
                       COUNT(su.id) FILTER (WHERE su.status = 'FAILED') AS failed,
                       COUNT(su.id) FILTER (WHERE su.status = 'PENDING' AND su.attempt_count > 0) AS retrying,
                       COUNT(su.id) FILTER (WHERE su.status = 'IN_PROGRESS' AND su.lease_expires_at < now())
                           AS stale_leases,
                       (SELECT COUNT(*) FROM enrichment_results er
                          JOIN store_units s2 ON s2.id = er.store_unit_id
                         WHERE s2.job_id = j.id
                           AND er.received_at > now() - make_interval(secs => ?)) AS recent_results
                FROM jobs j
                LEFT JOIN store_units su ON su.job_id = j.id
                GROUP BY j.id
                ORDER BY (j.status = 'RUNNING') DESC, j.created_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, i) -> {
            int total = rs.getInt("total_store_count");
            long succeeded = rs.getLong("succeeded");
            long failed = rs.getLong("failed");
            Timestamp lastActivity = rs.getTimestamp("last_activity_at");
            int percent = total == 0 ? 0 : (int) Math.round((succeeded + failed) * 100.0 / total);
            return new JobMonitorRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("status"),
                    rs.getString("source_filename"),
                    total,
                    rs.getTimestamp("created_at").toInstant(),
                    lastActivity == null ? null : lastActivity.toInstant(),
                    rs.getLong("pending"),
                    rs.getLong("in_progress"),
                    succeeded,
                    failed,
                    rs.getLong("retrying"),
                    rs.getLong("stale_leases"),
                    rs.getLong("recent_results") / (double) THROUGHPUT_WINDOW_SECONDS,
                    percent);
        }, THROUGHPUT_WINDOW_SECONDS);
    }

    public List<ErrorReason> topErrorReasons(UUID jobId, int limit) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT last_error, COUNT(*) AS n FROM store_units WHERE last_error IS NOT NULL"
                + jobFilter(jobId, args)
                + " GROUP BY last_error ORDER BY n DESC LIMIT ?";
        args.add(limit);
        return jdbcTemplate.query(
                sql, (rs, i) -> new ErrorReason(rs.getString("last_error"), rs.getLong("n")), args.toArray());
    }

    public List<RecentError> recentErrors(UUID jobId, int limit) {
        List<Object> args = new ArrayList<>();
        // claimed_at (set on every attempt) is used as the error time: store_units.updated_at is only
        // set at insert, so it can't order or date errors.
        String sql = "SELECT job_id, store_id, attempt_count, status, last_error,"
                + " COALESCE(claimed_at, updated_at) AS last_attempt_at FROM store_units"
                + " WHERE last_error IS NOT NULL"
                + jobFilter(jobId, args)
                + " ORDER BY COALESCE(claimed_at, updated_at) DESC LIMIT ?";
        args.add(limit);
        return jdbcTemplate.query(sql, (rs, i) -> {
            Timestamp lastAttempt = rs.getTimestamp("last_attempt_at");
            return new RecentError(
                    rs.getObject("job_id", UUID.class),
                    rs.getString("store_id"),
                    rs.getInt("attempt_count"),
                    rs.getString("status"),
                    rs.getString("last_error"),
                    lastAttempt.toInstant());
        }, args.toArray());
    }

    private static String jobFilter(UUID jobId, List<Object> args) {
        if (jobId == null) {
            return "";
        }
        args.add(jobId);
        return " AND job_id = ?";
    }
}
