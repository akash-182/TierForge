import { useState } from 'react'
import { formatClock, formatFull, timeAgo } from './formatTime'
import { getMonitorErrors } from './monitorApi'
import type { JobMonitorRow, RecentError } from './monitorTypes'
import { usePolling } from './usePolling'

interface Props {
  job: JobMonitorRow
  open: boolean
  onToggle: () => void
}

function formatEta(job: JobMonitorRow): string {
  const remaining = job.pending + job.inProgress
  if (job.status !== 'RUNNING' || remaining === 0 || job.throughputPerSec <= 0) {
    return '—'
  }
  const seconds = Math.round(remaining / job.throughputPerSec)
  return seconds >= 60 ? `~${Math.round(seconds / 60)} min` : `~${seconds}s`
}

export function JobPanel({ job, open, onToggle }: Props) {
  const [errors, setErrors] = useState<RecentError[]>([])

  usePolling(
    async () => {
      const result = await getMonitorErrors(job.id, 5)
      setErrors(result.recent)
    },
    3000,
    open,
  )

  return (
    <section className={`monitor-panel${open ? ' is-open' : ''}`}>
      <button
        type="button"
        className="monitor-panel-header"
        aria-expanded={open}
        onClick={onToggle}
      >
        <span className="monitor-chevron" aria-hidden="true">
          ▸
        </span>
        <span className="monitor-panel-title">
          <strong>{job.sourceFilename}</strong>
          <span className="monitor-muted"> · {job.id.slice(0, 8)}</span>
        </span>
        <span className={`monitor-badge status-${job.status.toLowerCase()}`}>{job.status}</span>
        <span className="monitor-mini-progress" aria-label={`${job.percentDone}% done`}>
          <span className="monitor-mini-fill" style={{ width: `${job.percentDone}%` }} />
        </span>
        <span className="monitor-percent">{job.percentDone}%</span>
      </button>

      <div className="monitor-panel-body">
        <div className="monitor-panel-inner">
          <dl className="counts monitor-counts">
            <div>
              <dt>Pending</dt>
              <dd>{job.pending}</dd>
            </div>
            <div>
              <dt>In progress</dt>
              <dd>{job.inProgress}</dd>
            </div>
            <div>
              <dt>Succeeded</dt>
              <dd>{job.succeeded}</dd>
            </div>
            <div>
              <dt>Failed</dt>
              <dd>{job.failed}</dd>
            </div>
          </dl>
          <dl className="counts monitor-counts monitor-secondary">
            <div>
              <dt>Retrying</dt>
              <dd>{job.retrying}</dd>
            </div>
            <div>
              <dt>Stale leases</dt>
              <dd>{job.staleLeases}</dd>
            </div>
            <div>
              <dt>Throughput</dt>
              <dd>{job.throughputPerSec.toFixed(1)}/s</dd>
            </div>
            <div>
              <dt>ETA</dt>
              <dd>{formatEta(job)}</dd>
            </div>
          </dl>

          <p className="monitor-muted monitor-times">
            <span title={formatFull(job.createdAt)}>Created {timeAgo(job.createdAt)}</span>
            {' · '}
            <span title={job.lastActivityAt ? formatFull(job.lastActivityAt) : undefined}>
              Last activity {timeAgo(job.lastActivityAt)}
            </span>
          </p>

          <h3 className="monitor-subtitle">Recent errors</h3>
          {errors.length === 0 ? (
            <p className="monitor-muted">No errors recorded for this job.</p>
          ) : (
            <ul className="monitor-error-list">
              {errors.map((error) => (
                <li key={`${error.storeId}-${error.lastAttemptAt}`}>
                  <span title={formatFull(error.lastAttemptAt)}>{formatClock(error.lastAttemptAt)}</span>{' '}
                  · <code>{error.storeId}</code> · attempt {error.attemptCount} · {error.status} ·{' '}
                  {error.lastError}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
