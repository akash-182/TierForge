import { useEffect, useState } from 'react'
import { ApiError, getJob, listFailedStoreUnits, startJob } from '../api/client'
import type { JobResponse, StoreUnitResponse } from '../api/types'

interface Props {
  job: JobResponse
  onJobUpdate: (job: JobResponse) => void
}

const POLL_INTERVAL_MS = 2000

export function JobProgress({ job, onJobUpdate }: Props) {
  const [starting, setStarting] = useState(false)
  const [startError, setStartError] = useState<string | null>(null)
  const [failedUnits, setFailedUnits] = useState<StoreUnitResponse[]>([])

  useEffect(() => {
    if (job.status !== 'RUNNING') {
      return
    }
    const interval = setInterval(async () => {
      try {
        const updated = await getJob(job.id)
        onJobUpdate(updated)
      } catch {
        // Transient poll failure — the next tick will retry; nothing to show the user for this.
      }
    }, POLL_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [job.status, job.id, onJobUpdate])

  useEffect(() => {
    if (job.status !== 'COMPLETED' || job.failed === 0) {
      setFailedUnits([])
      return
    }
    listFailedStoreUnits(job.id).then(setFailedUnits).catch(() => setFailedUnits([]))
  }, [job.status, job.failed, job.id])

  async function handleStart() {
    setStarting(true)
    setStartError(null)
    try {
      const updated = await startJob(job.id)
      onJobUpdate(updated)
    } catch (err) {
      setStartError(err instanceof ApiError ? err.messages.join('; ') : 'Failed to start job.')
    } finally {
      setStarting(false)
    }
  }

  const total = job.totalStoreCount || 1
  const donePercent = Math.round(((job.succeeded + job.failed) / total) * 100)

  return (
    <section className="card">
      <h2>2. Run enrichment job</h2>
      <p className="hint">
        {job.sourceFilename} — {job.totalStoreCount} stores — status: <strong>{job.status}</strong>
      </p>

      {job.status === 'PENDING' && (
        <button type="button" onClick={handleStart} disabled={starting}>
          {starting ? 'Starting…' : 'Start enrichment'}
        </button>
      )}
      {startError && <p className="error-text">{startError}</p>}

      {(job.status === 'RUNNING' || job.status === 'COMPLETED') && (
        <div className="progress">
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${donePercent}%` }} />
          </div>
          <dl className="counts">
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
        </div>
      )}

      {failedUnits.length > 0 && (
        <div className="failed-list">
          <h3>Failed stores</h3>
          <table>
            <thead>
              <tr>
                <th>Store ID</th>
                <th>Attempts</th>
                <th>Last error</th>
              </tr>
            </thead>
            <tbody>
              {failedUnits.map((unit) => (
                <tr key={unit.storeId}>
                  <td>{unit.storeId}</td>
                  <td>{unit.attemptCount}</td>
                  <td>{unit.lastError}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
