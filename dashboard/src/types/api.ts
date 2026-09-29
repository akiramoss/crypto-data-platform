// Mirrors the backend response DTOs (com.crypto_data_platform.dto.*). Keep in sync manually --
// there is no shared schema between the two projects.

export interface CryptoPriceResponse {
  symbol: string
  price: number
  marketCap: number
  volume: number
  eventTime: string
  timestamp: string
  priceFluctuation: number | null
}

export interface CryptoRankingEntry {
  symbol: string
  totalFluctuations: number
  positiveFluctuations: number
  gainDensityPercentage: number
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
