import type { ApiErrorResponse, CryptoPriceResponse, CryptoRankingEntry } from '../types/api'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string | undefined

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function get<T>(path: string): Promise<T> {
  // An empty string (set at Docker build time) means "same origin": nginx serves the app and
  // proxies /api/* to the backend, so requests are relative. Only a truly unset env var (local
  // dev without a .env file) is a misconfiguration.
  if (BASE_URL === undefined) {
    throw new ApiError('VITE_API_BASE_URL is not configured', 0)
  }

  const response = await fetch(`${BASE_URL}${path}`)

  if (!response.ok) {
    const message = await response
      .json()
      .then((body: ApiErrorResponse) => body.message)
      .catch(() => response.statusText)
    throw new ApiError(message, response.status)
  }

  return response.json() as Promise<T>
}

export function getLatestPrices(): Promise<CryptoPriceResponse[]> {
  return get('/api/cryptos/latest')
}

export function getPriceHistory(symbol: string, from?: string, to?: string): Promise<CryptoPriceResponse[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const query = params.toString()
  return get(`/api/cryptos/${encodeURIComponent(symbol)}/history${query ? `?${query}` : ''}`)
}

export function getRanking(minSamples?: number, limit?: number): Promise<CryptoRankingEntry[]> {
  const params = new URLSearchParams()
  if (minSamples !== undefined) params.set('minSamples', String(minSamples))
  if (limit !== undefined) params.set('limit', String(limit))
  const query = params.toString()
  return get(`/api/cryptos/ranking${query ? `?${query}` : ''}`)
}
