import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { OverviewPage } from './OverviewPage'

const { getLatestPrices } = vi.hoisted(() => ({ getLatestPrices: vi.fn() }))
vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, getLatestPrices }
})

function renderPage() {
  return render(
    <MemoryRouter>
      <OverviewPage />
    </MemoryRouter>,
  )
}

describe('OverviewPage', () => {
  beforeEach(() => {
    getLatestPrices.mockReset()
  })

  it('shows a loading state while the request is in flight', () => {
    getLatestPrices.mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent(/loading/i)
  })

  it('renders the price table once data arrives', async () => {
    getLatestPrices.mockResolvedValue([
      {
        symbol: 'btc',
        price: 66000,
        marketCap: 1_300_000_000_000,
        volume: 500_000_000,
        eventTime: '2024-01-15T10:30:00',
        timestamp: '2024-01-15T10:31:00',
        priceFluctuation: 1.5,
      },
    ])

    renderPage()

    expect(await screen.findByText('BTC')).toBeInTheDocument()
  })

  it('shows an empty state when there is no data yet', async () => {
    getLatestPrices.mockResolvedValue([])

    renderPage()

    await waitFor(() => expect(screen.getByText(/no price data/i)).toBeInTheDocument())
  })

  it('shows an error state with a retry button when the request fails', async () => {
    getLatestPrices.mockRejectedValue(new ApiError('Backend unavailable', 503))

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Backend unavailable')
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })
})
