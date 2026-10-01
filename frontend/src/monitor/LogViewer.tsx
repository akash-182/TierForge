import { useEffect, useRef, useState } from 'react'
import { formatClock, formatFull } from './formatTime'
import { getMonitorLogs } from './monitorApi'
import type { LogEntry } from './monitorTypes'
import { usePolling } from './usePolling'

const MAX_CLIENT_ENTRIES = 1000
const LEVELS = ['ALL', 'INFO', 'WARN', 'ERROR'] as const
type LevelFilter = (typeof LEVELS)[number]

const RANK: Record<string, number> = { DEBUG: 0, INFO: 1, WARN: 2, ERROR: 3 }

function passes(entry: LogEntry, filter: LevelFilter): boolean {
  return filter === 'ALL' || (RANK[entry.level] ?? 1) >= RANK[filter]
}

export function LogViewer() {
  const [entries, setEntries] = useState<LogEntry[]>([])
  const [filter, setFilter] = useState<LevelFilter>('ALL')
  const [paused, setPaused] = useState(false)
  const lastSeq = useRef(0)
  const container = useRef<HTMLDivElement>(null)
  const stickToBottom = useRef(true)

  usePolling(
    async () => {
      const result = await getMonitorLogs(lastSeq.current)
      // A lastSeq that went backwards means the backend restarted: start over.
      const restarted = result.lastSeq < lastSeq.current
      lastSeq.current = result.lastSeq
      if (result.entries.length === 0 && !restarted) {
        return
      }
      setEntries((current) =>
        (restarted ? result.entries : [...current, ...result.entries]).slice(-MAX_CLIENT_ENTRIES),
      )
    },
    1500,
    !paused,
  )

  useEffect(() => {
    const element = container.current
    if (element && stickToBottom.current) {
      element.scrollTop = element.scrollHeight
    }
  }, [entries, filter])

  function handleScroll() {
    const element = container.current
    if (element) {
      stickToBottom.current = element.scrollHeight - element.scrollTop - element.clientHeight < 24
    }
  }

  const visible = entries.filter((entry) => passes(entry, filter))

  return (
    <div className="monitor-logs">
      <div className="monitor-toolbar">
        <label>
          Level{' '}
          <select value={filter} onChange={(e) => setFilter(e.target.value as LevelFilter)}>
            {LEVELS.map((level) => (
              <option key={level} value={level}>
                {level === 'ALL' ? 'All' : `${level}+`}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="monitor-secondary-btn" onClick={() => setPaused(!paused)}>
          {paused ? 'Resume' : 'Pause'}
        </button>
        <button type="button" className="monitor-secondary-btn" onClick={() => setEntries([])}>
          Clear
        </button>
        <span className="monitor-muted">
          {paused ? 'Paused' : 'Live'} · {visible.length} lines
        </span>
      </div>
      <div className="monitor-log-box" ref={container} onScroll={handleScroll}>
        {visible.length === 0 ? (
          <p className="monitor-muted">No log lines yet.</p>
        ) : (
          visible.map((entry) => (
            <div key={entry.seq} className={`monitor-log-line level-${entry.level.toLowerCase()}`}>
              <span className="monitor-log-time" title={formatFull(entry.timestamp)}>
                {formatClock(entry.timestamp)}
              </span>
              <span className="monitor-log-level">{entry.level}</span>
              <span className="monitor-log-logger">{entry.logger}</span>
              <span className="monitor-log-message">{entry.message}</span>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
