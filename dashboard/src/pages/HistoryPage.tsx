import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getLatestPrices, getPriceHistory } from '../api/client'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { PriceHistoryChart } from '../components/PriceHistoryChart'
import { DASHBOARD_REFRESH_INTERVAL_MS } from '../config'
import { useAsync } from '../hooks/useAsync'

const RANGE_OPTIONS = [
  { label: '7 days', days: 7 },
  { label: '30 days', days: 30 },
  { label: '90 days', days: 90 },
] as const

export function HistoryPage() {
  const { symbol: symbolParam } = useParams<{ symbol?: string }>()
  const navigate = useNavigate()
  const [rangeDays, setRangeDays] = useState<number>(30)

  const { data: latestPrices } = useAsync(getLatestPrices, [], DASHBOARD_REFRESH_INTERVAL_MS)
  const availableSymbols = useMemo(() => (latestPrices ?? []).map((p) => p.symbol), [latestPrices])

  // Fall back to the first known symbol once the overview data has loaded, if none was chosen.
  const selectedSymbol = symbolParam ?? availableSymbols[0]

  const { from, to } = useMemo(() => {
    const to = new Date()
    const from = new Date(to)
    from.setDate(from.getDate() - rangeDays)
    return { from: from.toISOString().slice(0, 19), to: to.toISOString().slice(0, 19) }
  }, [rangeDays])

  const { data: history, loading, error, reload } = useAsync(
    () => (selectedSymbol ? getPriceHistory(selectedSymbol, from, to) : Promise.resolve([])),
    [selectedSymbol, from, to],
    DASHBOARD_REFRESH_INTERVAL_MS,
  )

  return (
    <section>
      <h2>Price history</h2>
      <div className="controls">
        <label>
          Symbol
          <select
            value={selectedSymbol ?? ''}
            onChange={(e) => navigate(`/history/${e.target.value}`)}
            disabled={availableSymbols.length === 0}
          >
            {availableSymbols.length === 0 && <option value="">No symbols available</option>}
            {availableSymbols.map((symbol) => (
              <option key={symbol} value={symbol}>
                {symbol.toUpperCase()}
              </option>
            ))}
          </select>
        </label>
        <div className="range-buttons">
          {RANGE_OPTIONS.map((option) => (
            <button
              key={option.days}
              type="button"
              className={option.days === rangeDays ? 'range-button range-button--active' : 'range-button'}
              onClick={() => setRangeDays(option.days)}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {!selectedSymbol && <EmptyState message="No symbols available yet." />}
      {selectedSymbol && loading && <LoadingState label={`Loading history for ${selectedSymbol.toUpperCase()}…`} />}
      {selectedSymbol && error && <ErrorState message={error} onRetry={reload} />}
      {selectedSymbol && !loading && !error && (!history || history.length === 0) && (
        <EmptyState message={`No price history for ${selectedSymbol.toUpperCase()} in this range.`} />
      )}
      {selectedSymbol && !loading && !error && history && history.length > 0 && (
        <PriceHistoryChart history={history} />
      )}
    </section>
  )
}
