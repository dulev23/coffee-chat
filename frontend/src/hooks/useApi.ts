import { useCallback, useEffect, useState } from 'react'

export function useApi<T>(fetcher: () => Promise<T>, errorMessage: string) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    try {
      setData(await fetcher())
      setError(null)
    } catch {
      setError(errorMessage)
    }
  }, [fetcher, errorMessage])

  useEffect(() => {
    reload()
  }, [reload])

  return { data, error, reload }
}
