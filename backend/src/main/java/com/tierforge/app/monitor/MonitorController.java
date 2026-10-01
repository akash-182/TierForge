package com.tierforge.app.monitor;

import com.tierforge.app.monitor.MonitorDtos.ErrorsResponse;
import com.tierforge.app.monitor.MonitorDtos.JobMonitorRow;
import com.tierforge.app.monitor.MonitorDtos.LogsResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Read-only endpoints backing the frontend's "Live monitor" page. Never calls the enrichment API.
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final int MAX_LIMIT = 200;

    private final MonitorQueryDao queryDao;
    private final LogBuffer logBuffer;

    public MonitorController(MonitorQueryDao queryDao, LogBuffer logBuffer) {
        this.queryDao = queryDao;
        this.logBuffer = logBuffer;
    }

    @GetMapping("/jobs")
    public List<JobMonitorRow> jobs() {
        return queryDao.listJobs();
    }

    @GetMapping("/logs")
    public LogsResponse logs(
            @RequestParam(defaultValue = "0") long after, @RequestParam(defaultValue = "200") int limit) {
        // A cursor ahead of the buffer means the backend restarted (sequence numbers reset), so
        // start over rather than waiting forever for a seq that will never be reached.
        long effectiveAfter = after > logBuffer.lastSeq() || after < 0 ? 0 : after;
        List<LogEntry> entries = logBuffer.since(effectiveAfter, clamp(limit));
        long lastSeq = entries.isEmpty() ? effectiveAfter : entries.get(entries.size() - 1).seq();
        return new LogsResponse(entries, lastSeq);
    }

    @GetMapping("/errors")
    public ErrorsResponse errors(
            @RequestParam(required = false) UUID jobId, @RequestParam(defaultValue = "50") int limit) {
        int bounded = clamp(limit);
        return new ErrorsResponse(queryDao.topErrorReasons(jobId, 10), queryDao.recentErrors(jobId, bounded));
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
