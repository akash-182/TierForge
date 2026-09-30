import { useEffect, useState } from 'react'
import { listScores } from '../api/client'
import type { ScoredStoreResponse, ScoringSummaryResponse, StoreTier } from '../api/types'

interface Props {
  jobId: string
  summary: ScoringSummaryResponse
}

export function Dashboard({ jobId, summary }: Props) {
  const [tierFilter, setTierFilter] = useState<StoreTier | 'ALL'>('ALL')
  const [stores, setStores] = useState<ScoredStoreResponse[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    listScores(jobId, tierFilter === 'ALL' ? undefined : tierFilter)
      .then(setStores)
      .catch(() => setStores([]))
      .finally(() => setLoading(false))
    // summary changes every time scoring is resubmitted — refetch so the list reflects it.
  }, [jobId, tierFilter, summary])

  const { large, medium, small } = summary.tierBreakdown
  const total = large + medium + small
  const pct = (count: number) => (total === 0 ? 0 : Math.round((count / total) * 100))

  return (
    <section className="card">
      <h2>4. Dashboard</h2>

      <div className="tier-breakdown">
        <div className="tier-tile tier-large">
          <span className="tier-count">{large}</span>
          <span className="tier-pct">{pct(large)}%</span>
          <span>Large</span>
        </div>
        <div className="tier-tile tier-medium">
          <span className="tier-count">{medium}</span>
          <span className="tier-pct">{pct(medium)}%</span>
          <span>Medium</span>
        </div>
        <div className="tier-tile tier-small">
          <span className="tier-count">{small}</span>
          <span className="tier-pct">{pct(small)}%</span>
          <span>Small</span>
        </div>
      </div>

      <label className="tier-filter">
        Filter by tier:
        <select value={tierFilter} onChange={(e) => setTierFilter(e.target.value as StoreTier | 'ALL')}>
          <option value="ALL">All</option>
          <option value="LARGE">Large</option>
          <option value="MEDIUM">Medium</option>
          <option value="SMALL">Small</option>
        </select>
      </label>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Store</th>
              <th>Footfall</th>
              <th>Revenue</th>
              <th>Size (sqft)</th>
              <th>Score</th>
              <th>Tier</th>
            </tr>
          </thead>
          <tbody>
            {stores.map((store) => (
              <tr key={store.storeId}>
                <td>{store.storeName}</td>
                <td>{store.footfall.toLocaleString()}</td>
                <td>{store.revenue.toLocaleString(undefined, { maximumFractionDigits: 2 })}</td>
                <td>{store.sqft.toLocaleString()}</td>
                <td>{store.score}%</td>
                <td>
                  <span className={`tier-badge tier-badge-${store.tier.toLowerCase()}`}>{store.tier}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
