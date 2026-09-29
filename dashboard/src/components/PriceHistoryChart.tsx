import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { CryptoPriceResponse } from '../types/api'
import { formatCurrency, formatDateTime } from '../utils/format'

interface PriceHistoryChartProps {
  history: CryptoPriceResponse[]
}

export function PriceHistoryChart({ history }: PriceHistoryChartProps) {
  // The API returns newest-first; the chart reads left-to-right chronologically.
  const chronological = [...history].reverse().map((point) => ({
    eventTime: point.eventTime,
    price: point.price,
  }))

  return (
    <ResponsiveContainer width="100%" height={360}>
      <LineChart data={chronological}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="eventTime" tickFormatter={(value: string) => formatDateTime(value)} minTickGap={40} />
        <YAxis tickFormatter={(value: number) => formatCurrency(value)} width={90} />
        <Tooltip
          labelFormatter={(value) => formatDateTime(String(value))}
          formatter={(value) => formatCurrency(Number(value))}
        />
        <Line type="monotone" dataKey="price" stroke="#2563eb" dot={false} strokeWidth={2} />
      </LineChart>
    </ResponsiveContainer>
  )
}
