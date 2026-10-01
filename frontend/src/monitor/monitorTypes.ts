export interface JobMonitorRow {
  id: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  sourceFilename: string
  totalStoreCount: number
  createdAt: string
  lastActivityAt: string | null
  pending: number
  inProgress: number
  succeeded: number
  failed: number
  retrying: number
  staleLeases: number
  throughputPerSec: number
  percentDone: number
}

export interface LogEntry {
  seq: number
  timestamp: string
  level: string
  logger: string
  message: string
}

export interface LogsResponse {
  entries: LogEntry[]
  lastSeq: number
}

export interface ErrorReason {
  reason: string
  count: number
}

export interface RecentError {
  jobId: string
  storeId: string
  attemptCount: number
  status: string
  lastError: string
  lastAttemptAt: string
}

export interface ErrorsResponse {
  reasons: ErrorReason[]
  recent: RecentError[]
}
