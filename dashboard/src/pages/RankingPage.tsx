import { useState } from 'react'
import { getRanking } from '../api/client'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { RankingTable } from '../components/RankingTable'
import { useAsync } from '../hooks/useAsync'

const DEFAULT_MIN_SAMPLES = 3
const DEFAULT_LIMIT = 10

export function RankingPage() {
  const [minSamples, setMinSamples] = useState(DEFAULT_MIN_SAMPLES)
  const [limit, setLimit] = useState(DEFAULT_LIMIT)

  const { data, loading, error, reload } = useAsync(() => getRanking(minSamples, limit), [minSamples, limit])

  return (
    <section>
      <h2>Most consistently bullish</h2>
      <div className="controls">
        <label>
          Min. samples
          <input
            type="number"
            min={0}
            value={minSamples}
            onChange={(e) => setMinSamples(Math.max(0, Number(e.target.value)))}
          />
        </label>
        <label>
          Limit
          <input type="number" min={1} value={limit} onChange={(e) => setLimit(Math.max(1, Number(e.target.value)))} />
        </label>
      </div>

      {loading && <LoadingState label="Loading ranking…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && (!data || data.length === 0) && (
        <EmptyState message="No symbol has enough samples yet for this minimum." />
      )}
      {!loading && !error && data && data.length > 0 && <RankingTable entries={data} />}
    </section>
  )
}
