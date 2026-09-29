import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, submitScoring } from '../api/client'
import type { ScoringConfigInput, ScoringSummaryResponse } from '../api/types'

interface Props {
  jobId: string
  onScored: (summary: ScoringSummaryResponse) => void
}

const DEFAULTS: ScoringConfigInput = {
  footfallBar: 15000,
  footfallWeight: 50,
  revenueBar: 150000,
  revenueWeight: 30,
  sizeBar: 8000,
  sizeWeight: 20,
  tierLargeThreshold: 70,
  tierMediumThreshold: 40,
}

export function ScoringConfigForm({ jobId, onScored }: Props) {
  const [config, setConfig] = useState<ScoringConfigInput>(DEFAULTS)
  const [errors, setErrors] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  const weightSum = config.footfallWeight + config.revenueWeight + config.sizeWeight

  function setField(field: keyof ScoringConfigInput, value: number) {
    setConfig((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setErrors([])
    try {
      const summary = await submitScoring(jobId, config)
      onScored(summary)
    } catch (err) {
      setErrors(err instanceof ApiError ? err.messages : ['Failed to submit scoring config.'])
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h2>3. Configure scoring &amp; tiering</h2>
      <p className="hint">
        Set a bar and weight per metric — weights must sum to 100%. A store's score is the sum of
        the weights for every bar it clears.
      </p>
      <form onSubmit={handleSubmit} className="scoring-form">
        <div className="metric-row">
          <label>
            Footfall bar
            <input
              type="number"
              value={config.footfallBar}
              onChange={(e) => setField('footfallBar', Number(e.target.value))}
            />
          </label>
          <label>
            Weight %
            <input
              type="number"
              value={config.footfallWeight}
              onChange={(e) => setField('footfallWeight', Number(e.target.value))}
            />
          </label>
        </div>
        <div className="metric-row">
          <label>
            Revenue bar
            <input
              type="number"
              value={config.revenueBar}
              onChange={(e) => setField('revenueBar', Number(e.target.value))}
            />
          </label>
          <label>
            Weight %
            <input
              type="number"
              value={config.revenueWeight}
              onChange={(e) => setField('revenueWeight', Number(e.target.value))}
            />
          </label>
        </div>
        <div className="metric-row">
          <label>
            Size bar (sqft)
            <input
              type="number"
              value={config.sizeBar}
              onChange={(e) => setField('sizeBar', Number(e.target.value))}
            />
          </label>
          <label>
            Weight %
            <input
              type="number"
              value={config.sizeWeight}
              onChange={(e) => setField('sizeWeight', Number(e.target.value))}
            />
          </label>
        </div>

        <p className={weightSum === 100 ? 'weight-sum ok' : 'weight-sum bad'}>
          Weights sum to {weightSum}% {weightSum === 100 ? '✓' : '(must be 100)'}
        </p>

        <div className="metric-row">
          <label>
            Large tier threshold %
            <input
              type="number"
              value={config.tierLargeThreshold}
              onChange={(e) => setField('tierLargeThreshold', Number(e.target.value))}
            />
          </label>
          <label>
            Medium tier threshold %
            <input
              type="number"
              value={config.tierMediumThreshold}
              onChange={(e) => setField('tierMediumThreshold', Number(e.target.value))}
            />
          </label>
        </div>

        <button type="submit" disabled={submitting || weightSum !== 100}>
          {submitting ? 'Scoring…' : 'Compute scores'}
        </button>
      </form>
      {errors.length > 0 && (
        <ul className="error-list">
          {errors.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
