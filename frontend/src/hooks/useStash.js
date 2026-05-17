import { useState, useCallback } from 'react'
import * as api from '../api/index.js'

export function useStash(code) {
  const [stash, setStash] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await api.viewStash(code)
      setStash(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [code])

  const withRefresh = useCallback((fn) => async (...args) => {
    setError(null)
    try {
      await fn(...args)
      await refresh()
    } catch (e) {
      setError(e.message)
    }
  }, [refresh])

  return { stash, loading, error, refresh, withRefresh, setError }
}
