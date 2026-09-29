import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HistoryPage } from './HistoryPage'

const { getLatestPrices, getPriceHistory } = vi.hoisted(() => ({
  getLatestPrices: vi.fn(),
  getPriceHistory: vi.fn(),
}))
vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, getLatestPrices, getPriceHistory }
})

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/history/:symbol" element={<HistoryPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('HistoryPage', () => {
  beforeEach(() => {
    getLatestPrices.mockReset()
    getPriceHistory.mockReset()
    getLatestPrices.mockResolvedValue([
      { symbol: 'btc', price: 66000, marketCap: 1, volume: 1, eventTime: '', timestamp: '', priceFluctuation: null },
      { symbol: 'eth', price: 3000, marketCap: 1, volume: 1, eventTime: '', timestamp: '', priceFluctuation: null },
    ])
  })

  it('uses the symbol from the URL when one is given', async () => {
    getPriceHistory.mockResolvedValue([
      {
        symbol: 'eth',
        price: 3000,
        marketCap: 1,
        volume: 1,
        eventTime: '2024-01-15T10:30:00',
        timestamp: '2024-01-15T10:30:00',
        priceFluctuation: null,
      },
    ])

    renderAt('/history/eth')

    await waitFor(() =>
      expect(getPriceHistory).toHaveBeenCalledWith('eth', expect.any(String), expect.any(String)),
    )
  })

  it('falls back to the first available symbol when none is given in the URL', async () => {
    getPriceHistory.mockResolvedValue([])

    renderAt('/history')

    await waitFor(() =>
      expect(getPriceHistory).toHaveBeenCalledWith('btc', expect.any(String), expect.any(String)),
    )
  })

  it('shows an empty state when there is no history in the selected range', async () => {
    getPriceHistory.mockResolvedValue([])

    renderAt('/history/btc')

    expect(await screen.findByText(/no price history for btc/i)).toBeInTheDocument()
  })
})
