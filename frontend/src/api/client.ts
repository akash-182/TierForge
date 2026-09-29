import type {
  JobResponse,
  ScoredStoreResponse,
  ScoringConfigInput,
  ScoringSummaryResponse,
  StoreTier,
  StoreUnitResponse,
} from './types'

// The backend returns either {"errors": [...]} (CSV validation) or {"error": "..."} (everything
// else) on failure — this normalizes both into a single list so callers don't care which shape.
export class ApiError extends Error {
  readonly messages: string[]

  constructor(messages: string[]) {
    super(messages.join('; '))
    this.messages = messages
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    let messages: string[] = [`Request failed: ${response.status}`]
    try {
      const body = await response.json()
      if (Array.isArray(body.errors)) {
        messages = body.errors
      } else if (typeof body.error === 'string') {
        messages = [body.error]
      }
    } catch {
      // Response body wasn't JSON — keep the generic status-based message.
    }
    throw new ApiError(messages)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function uploadJob(file: File): Promise<JobResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return request<JobResponse>('/api/jobs', { method: 'POST', body: formData })
}

export function startJob(jobId: string): Promise<JobResponse> {
  return request<JobResponse>(`/api/jobs/${jobId}/start`, { method: 'POST' })
}

export function getJob(jobId: string): Promise<JobResponse> {
  return request<JobResponse>(`/api/jobs/${jobId}`)
}

export function listFailedStoreUnits(jobId: string): Promise<StoreUnitResponse[]> {
  return request<StoreUnitResponse[]>(`/api/jobs/${jobId}/store-units?status=FAILED`)
}

export function submitScoring(
  jobId: string,
  config: ScoringConfigInput,
): Promise<ScoringSummaryResponse> {
  return request<ScoringSummaryResponse>(`/api/jobs/${jobId}/scoring`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config),
  })
}

export function listScores(jobId: string, tier?: StoreTier): Promise<ScoredStoreResponse[]> {
  const query = tier ? `?tier=${tier}` : ''
  return request<ScoredStoreResponse[]>(`/api/jobs/${jobId}/scores${query}`)
}
