import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '@/lib/api'

export interface DLQEntry {
  id: number
  eventId: number
  reason: string
  payload: string
  createdAt: string
  replayedAt: string | null
}

interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
}

export function useDLQ(page = 0, size = 20) {
  const [data, setData] = useState<Page<DLQEntry> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const result = await apiFetch<Page<DLQEntry>>(`/api/v1/dlq?page=${page}&size=${size}`)
      setData(result)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load DLQ events')
    } finally {
      setLoading(false)
    }
  }, [page, size])

  useEffect(() => { fetchData() }, [fetchData])

  async function reprocess(id: number): Promise<DLQEntry> {
    const result = await apiFetch<DLQEntry>(`/api/v1/dlq/${id}/reprocess`, { method: 'POST' })
    await fetchData()
    return result
  }

  return { data, loading, error, reprocess, refresh: fetchData }
}
