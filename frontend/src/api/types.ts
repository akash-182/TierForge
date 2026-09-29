export type JobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface JobResponse {
  id: string
  status: JobStatus
  sourceFilename: string
  totalStoreCount: number
  createdAt: string
  pending: number
  inProgress: number
  succeeded: number
  failed: number
}

export interface StoreUnitResponse {
  storeId: string
  status: string
  attemptCount: number
  lastError: string | null
}

export interface ScoringConfigInput {
  footfallBar: number
  footfallWeight: number
  revenueBar: number
  revenueWeight: number
  sizeBar: number
  sizeWeight: number
  tierLargeThreshold: number
  tierMediumThreshold: number
}

export interface ScoringConfigResponse extends ScoringConfigInput {
  updatedAt: string
}

export interface TierBreakdown {
  large: number
  medium: number
  small: number
}

export interface ScoringSummaryResponse {
  config: ScoringConfigResponse
  tierBreakdown: TierBreakdown
  scoredStoreCount: number
}

export type StoreTier = 'LARGE' | 'MEDIUM' | 'SMALL'

export interface ScoredStoreResponse {
  storeId: string
  storeName: string
  footfall: number
  revenue: number
  sqft: number
  score: number
  tier: StoreTier
}
