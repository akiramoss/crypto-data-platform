import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RankingPage } from './RankingPage'

const { getRanking } = vi.hoisted(() => ({ getRanking: vi.fn() }))
vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, getRanking }
})

describe('RankingPage', () => {
  beforeEach(() => {
    getRanking.mockReset()
    getRanking.mockResolvedValue([
      { symbol: 'btc', totalFluctuations: 10, positiveFluctuations: 8, gainDensityPercentage: 80 },
    ])
  })

  it('loads the ranking with the default minSamples and limit', async () => {
    render(<RankingPage />)

    expect(await screen.findByText('BTC')).toBeInTheDocument()
    expect(getRanking).toHaveBeenCalledWith(3, 10)
  })

  it('reloads with the new minSamples when the input changes', async () => {
    render(<RankingPage />)
    await screen.findByText('BTC')

    fireEvent.change(screen.getByLabelText(/min\. samples/i), { target: { value: '5' } })

    expect(getRanking).toHaveBeenCalledWith(5, 10)
  })

  it('shows an empty state when no symbol qualifies', async () => {
    getRanking.mockResolvedValue([])

    render(<RankingPage />)

    expect(await screen.findByText(/no symbol has enough samples/i)).toBeInTheDocument()
  })
})
