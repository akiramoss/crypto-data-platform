import { describe, expect, it } from 'vitest'
import { formatCompactCurrency, formatCurrency, formatDateTime, formatPercentage } from './format'

describe('formatCurrency', () => {
  it('formats a typical price with two decimals', () => {
    expect(formatCurrency(65000.5)).toBe('$65,000.50')
  })

  it('keeps more decimals for very small prices', () => {
    expect(formatCurrency(0.000123)).toBe('$0.000123')
  })
})

describe('formatCompactCurrency', () => {
  it('formats a trillion-scale market cap in compact notation', () => {
    expect(formatCompactCurrency(1_690_000_000_000)).toBe('$1.69T')
  })

  it('formats a billion-scale value', () => {
    expect(formatCompactCurrency(52_300_000_000)).toBe('$52.3B')
  })
})

describe('formatPercentage', () => {
  it('formats a positive fluctuation with a leading sign', () => {
    expect(formatPercentage(1.54)).toBe('+1.54%')
  })

  it('formats a negative fluctuation', () => {
    expect(formatPercentage(-0.5)).toBe('-0.5%')
  })

  it('formats exactly zero without a sign', () => {
    expect(formatPercentage(0)).toBe('0.0%')
  })
})

describe('formatDateTime', () => {
  it('formats an ISO string into a readable date and time', () => {
    const formatted = formatDateTime('2024-01-15T10:30:00')
    expect(formatted).toContain('2024')
    expect(formatted).toMatch(/Jan/)
  })
})
