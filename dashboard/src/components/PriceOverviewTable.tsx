import { Link } from 'react-router-dom'
import type { CryptoPriceResponse } from '../types/api'
import { formatCompactCurrency, formatCurrency, formatDateTime, formatPercentage } from '../utils/format'

interface PriceOverviewTableProps {
  prices: CryptoPriceResponse[]
}

export function PriceOverviewTable({ prices }: PriceOverviewTableProps) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Price</th>
            <th>24h change</th>
            <th>Market cap</th>
            <th>Volume</th>
            <th>Last updated</th>
          </tr>
        </thead>
        <tbody>
          {prices.map((price) => (
            <tr key={price.symbol}>
              <td>
                <Link to={`/history/${price.symbol}`}>{price.symbol.toUpperCase()}</Link>
              </td>
              <td>{formatCurrency(price.price)}</td>
              <td className={fluctuationClass(price.priceFluctuation)}>
                {price.priceFluctuation !== null ? formatPercentage(price.priceFluctuation) : '—'}
              </td>
              <td>{formatCompactCurrency(price.marketCap)}</td>
              <td>{formatCompactCurrency(price.volume)}</td>
              <td>{formatDateTime(price.eventTime)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function fluctuationClass(fluctuation: number | null): string {
  if (fluctuation === null) return ''
  return fluctuation > 0 ? 'positive' : fluctuation < 0 ? 'negative' : ''
}
