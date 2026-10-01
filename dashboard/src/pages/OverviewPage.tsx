import { getLatestPrices } from '../api/client'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { PriceOverviewTable } from '../components/PriceOverviewTable'
import { DASHBOARD_REFRESH_INTERVAL_MS } from '../config'
import { useAsync } from '../hooks/useAsync'

export function OverviewPage() {
  const { data, loading, error, reload } = useAsync(getLatestPrices, [], DASHBOARD_REFRESH_INTERVAL_MS)

  if (loading) return <LoadingState label="Loading latest prices…" />
  if (error) return <ErrorState message={error} onRetry={reload} />
  if (!data || data.length === 0) return <EmptyState message="No price data has been ingested yet." />

  return (
    <section>
      <h2>Latest prices</h2>
      <PriceOverviewTable prices={data} />
    </section>
  )
}
