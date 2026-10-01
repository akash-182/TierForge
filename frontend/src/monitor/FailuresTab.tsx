import { useState } from 'react'
import { formatClock, formatFull, timeAgo } from './formatTime'
import { getMonitorErrors } from './monitorApi'
import type { ErrorsResponse } from './monitorTypes'
import { usePolling } from './usePolling'

export function FailuresTab({ active }: { active: boolean }) {
  const [data, setData] = useState<ErrorsResponse | null>(null)

  usePolling(
    async () => {
      setData(await getMonitorErrors(undefined, 50))
    },
    3000,
    active,
  )

  if (!data) {
    return <p className="monitor-muted">Loading…</p>
  }

  return (
    <div className="monitor-failures">
      <h3 className="monitor-subtitle">Failure reasons</h3>
      {data.reasons.length === 0 ? (
        <p className="monitor-muted">No errors recorded.</p>
      ) : (
        <ul className="monitor-reasons">
          {data.reasons.map((reason) => (
            <li key={reason.reason}>
              <span className="monitor-reason-count">{reason.count}</span>
              <span>{reason.reason}</span>
            </li>
          ))}
        </ul>
      )}

      <h3 className="monitor-subtitle">Recent errors</h3>
      {data.recent.length > 0 && (
        <div className="monitor-table-wrap">
          <table>
            <thead>
              <tr>
                <th>Last attempt</th>
                <th>Job</th>
                <th>Store</th>
                <th>Attempts</th>
                <th>Status</th>
                <th>Error</th>
              </tr>
            </thead>
            <tbody>
              {data.recent.map((error) => (
                <tr key={`${error.jobId}-${error.storeId}-${error.lastAttemptAt}`}>
                  <td title={formatFull(error.lastAttemptAt)}>
                    {formatClock(error.lastAttemptAt)}
                    <span className="monitor-muted"> · {timeAgo(error.lastAttemptAt)}</span>
                  </td>
                  <td>
                    <code>{error.jobId.slice(0, 8)}</code>
                  </td>
                  <td>{error.storeId}</td>
                  <td>{error.attemptCount}</td>
                  <td>{error.status}</td>
                  <td>{error.lastError}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
