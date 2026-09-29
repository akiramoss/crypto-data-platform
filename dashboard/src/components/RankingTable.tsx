import type { CryptoRankingEntry } from '../types/api'
import { formatPercentage } from '../utils/format'

interface RankingTableProps {
  entries: CryptoRankingEntry[]
}

export function RankingTable({ entries }: RankingTableProps) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Symbol</th>
            <th>Gain density</th>
            <th>Positive / total</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry, index) => (
            <tr key={entry.symbol}>
              <td>{index + 1}</td>
              <td>{entry.symbol.toUpperCase()}</td>
              <td>{formatPercentage(entry.gainDensityPercentage)}</td>
              <td>
                {entry.positiveFluctuations} / {entry.totalFluctuations}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
