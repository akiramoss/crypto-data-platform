import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client'

interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => void
}

/**
 * Fetches on mount and whenever `deps` change; exposes a manual `reload` for retry buttons.
 * Pass `refreshIntervalMs` to also poll silently in the background: loading/error state is left
 * untouched by these polls, so the UI doesn't flicker and a failed poll just keeps the last good
 * data on screen instead of replacing it with an error.
 */
export function useAsync<T>(fetcher: () => Promise<T>, deps: unknown[], refreshIntervalMs?: number): AsyncState<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  const load = useCallback(
    (silent = false) => {
      let cancelled = false
      if (!silent) {
        setLoading(true)
        setError(null)
      }

      fetcher()
        .then((result) => {
          if (!cancelled) setData(result)
        })
        .catch((err: unknown) => {
          if (cancelled || silent) return
          setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
        })
        .finally(() => {
          if (!cancelled && !silent) setLoading(false)
        })

      return () => {
        cancelled = true
      }
      // `deps` intentionally drives re-fetching directly; callers pass their own dependency array.
    },
    [...deps, reloadToken],
  )

  useEffect(() => load(), [load])

  useEffect(() => {
    if (!refreshIntervalMs) return undefined
    const id = setInterval(() => load(true), refreshIntervalMs)
    return () => clearInterval(id)
  }, [load, refreshIntervalMs])

  const reload = useCallback(() => setReloadToken((token) => token + 1), [])

  return { data, loading, error, reload }
}
