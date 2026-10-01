import type { ErrorsResponse, JobMonitorRow, LogsResponse } from './monitorTypes'

// Deliberately its own small fetch wrapper (not api/client.ts) so the monitor stays an
// independent add-on to the core upload/enrich/score flow.
async function get<T>(path: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(`Monitor request failed: ${response.status}`)
  }
  return (await response.json()) as T
}

export function getMonitorJobs(): Promise<JobMonitorRow[]> {
  return get('/api/monitor/jobs')
}

export function getMonitorLogs(after: number): Promise<LogsResponse> {
  return get(`/api/monitor/logs?after=${after}&limit=200`)
}

export function getMonitorErrors(jobId?: string, limit = 50): Promise<ErrorsResponse> {
  const params = new URLSearchParams({ limit: String(limit) })
  if (jobId) {
    params.set('jobId', jobId)
  }
  return get(`/api/monitor/errors?${params}`)
}
