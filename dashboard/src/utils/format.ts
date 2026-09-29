const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 6,
})

const compactCurrencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  notation: 'compact',
  maximumFractionDigits: 2,
})

const percentFormatter = new Intl.NumberFormat('en-US', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
  signDisplay: 'exceptZero',
})

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/** Full precision, e.g. for the current price of a single unit: "$65,000.50". */
export function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

/** Compact notation for large magnitudes (market cap, volume), e.g. "$1.69T". */
export function formatCompactCurrency(value: number): string {
  return compactCurrencyFormatter.format(value)
}

/** Expects a fraction of 1 (e.g. 1.5 for "+150.0%"), matching priceFluctuation's own units. */
export function formatPercentage(value: number): string {
  return percentFormatter.format(value / 100)
}

export function formatDateTime(isoString: string): string {
  return dateTimeFormatter.format(new Date(isoString))
}
