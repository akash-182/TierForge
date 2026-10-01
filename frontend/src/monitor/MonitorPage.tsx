import { useState } from 'react'
import './monitor.css'
import { FailuresTab } from './FailuresTab'
import { JobPanel } from './JobPanel'
import { LogViewer } from './LogViewer'
import { getMonitorJobs } from './monitorApi'
import type { JobMonitorRow } from './monitorTypes'
import { usePolling } from './usePolling'

type Tab = 'jobs' | 'logs' | 'failures'
type JobFilter = 'latest' | 'active' | 'all'

const FILTERS: { id: JobFilter; label: string }[] = [
  { id: 'latest', label: 'Latest' },
  { id: 'active', label: 'Active only' },
  { id: 'all', label: 'All' },
]

function applyFilter(jobs: JobMonitorRow[], filter: JobFilter): JobMonitorRow[] {
  if (filter === 'all') {
    return jobs
  }
  if (filter === 'active') {
    return jobs.filter((job) => job.status === 'RUNNING')
  }
  // The list is ordered running-first, so pick the most recently created job explicitly.
  const newest = jobs.reduce<JobMonitorRow | null>(
    (best, job) => (best === null || job.createdAt > best.createdAt ? job : best),
    null,
  )
  return newest ? [newest] : []
}

const TABS: { id: Tab; label: string }[] = [
  { id: 'jobs', label: 'Jobs' },
  { id: 'logs', label: 'Logs' },
  { id: 'failures', label: 'Failures' },
]

export function MonitorPage({ onBack }: { onBack: () => void }) {
  const [tab, setTab] = useState<Tab>('jobs')
  const [filter, setFilter] = useState<JobFilter>('latest')
  const [jobs, setJobs] = useState<JobMonitorRow[] | null>(null)
  const [connectionLost, setConnectionLost] = useState(false)
  // Explicit open/closed choices; jobs without one default to open while RUNNING.
  const [overrides, setOverrides] = useState<Record<string, boolean>>({})

  usePolling(async () => {
    try {
      setJobs(await getMonitorJobs())
      setConnectionLost(false)
    } catch (err) {
      setConnectionLost(true)
      throw err
    }
  }, 2000)

  function isOpen(job: JobMonitorRow): boolean {
    return overrides[job.id] ?? job.status === 'RUNNING'
  }

  const visibleJobs = applyFilter(jobs ?? [], filter)

  function setAll(open: boolean) {
    setOverrides({
      ...overrides,
      ...Object.fromEntries(visibleJobs.map((job) => [job.id, open])),
    })
  }

  const running = jobs?.filter((job) => job.status === 'RUNNING').length ?? 0

  function emptyMessage(): string {
    if (filter === 'active') {
      return 'No jobs are running right now.'
    }
    return 'No jobs yet — upload a store list first.'
  }

  return (
    <div className="monitor">
      <button type="button" className="reset-link" onClick={onBack}>
        ← Back
      </button>
      <section className="card">
        <div className="monitor-heading">
          <h2>Live monitor</h2>
          <span className="monitor-muted">
            {running} running · {jobs?.length ?? 0} total
          </span>
        </div>
        {connectionLost && (
          <p className="error-text">Can't reach the backend — retrying…</p>
        )}

        <div className="monitor-tabs" role="tablist">
          {TABS.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={tab === id}
              className={`monitor-tab${tab === id ? ' is-active' : ''}`}
              onClick={() => setTab(id)}
            >
              {label}
            </button>
          ))}
        </div>

        <div hidden={tab !== 'jobs'}>
          <div className="monitor-toolbar">
            <div className="monitor-filter" role="group" aria-label="Show jobs">
              {FILTERS.map(({ id, label }) => (
                <button
                  key={id}
                  type="button"
                  aria-pressed={filter === id}
                  className={`monitor-secondary-btn monitor-filter-btn${filter === id ? ' is-active' : ''}`}
                  onClick={() => setFilter(id)}
                >
                  {label}
                </button>
              ))}
            </div>
            <button type="button" className="monitor-secondary-btn" onClick={() => setAll(true)}>
              Expand all
            </button>
            <button type="button" className="monitor-secondary-btn" onClick={() => setAll(false)}>
              Collapse all
            </button>
          </div>
          {jobs === null && <p className="monitor-muted">Loading…</p>}
          {jobs !== null && visibleJobs.length === 0 && (
            <p className="monitor-muted">{emptyMessage()}</p>
          )}
          {jobs !== null && filter !== 'all' && jobs.length > visibleJobs.length && (
            <p className="monitor-muted monitor-filter-note">
              Showing {visibleJobs.length} of {jobs.length} jobs.{' '}
              <button type="button" className="reset-link" onClick={() => setFilter('all')}>
                Show all
              </button>
            </p>
          )}
          <div className="monitor-panels">
            {visibleJobs.map((job) => (
              <JobPanel
                key={job.id}
                job={job}
                open={isOpen(job)}
                onToggle={() => setOverrides({ ...overrides, [job.id]: !isOpen(job) })}
              />
            ))}
          </div>
        </div>

        <div hidden={tab !== 'logs'}>
          <LogViewer />
        </div>

        <div hidden={tab !== 'failures'}>
          <FailuresTab active={tab === 'failures'} />
        </div>
      </section>
    </div>
  )
}
