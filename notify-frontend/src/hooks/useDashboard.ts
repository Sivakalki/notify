import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '@/lib/api'

export interface DashboardMetrics {
  totalSent: number
  totalFailed: number
  totalPending: number
  totalDuplicates: number
  totalCampaigns: number
  cachedAt: string
}

export interface ConsumerLagEntry {
  groupId: string
  topic: string
  partition: number
  lag: number
}

export interface DashboardFilters {
  startDate: string
  endDate: string
  campaignIds: number[]
  uploadFileName: string
}

export interface CampaignOption {
  id: number
  campaignName: string
}

export const EMPTY_FILTERS: DashboardFilters = {
  startDate: '',
  endDate: '',
  campaignIds: [],
  uploadFileName: '',
}

function buildMetricsUrl(f: DashboardFilters): string {
  const p = new URLSearchParams()
  if (f.startDate) p.set('startDate', f.startDate)
  if (f.endDate) p.set('endDate', f.endDate)
  f.campaignIds.forEach((id) => p.append('campaignIds', String(id)))
  if (f.uploadFileName) p.set('uploadFileName', f.uploadFileName)
  const qs = p.toString()
  return `/api/v1/dashboard/metrics${qs ? `?${qs}` : ''}`
}

export function useDashboard(filters: DashboardFilters) {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null)
  const [lag, setLag] = useState<ConsumerLagEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastRefresh, setLastRefresh] = useState(new Date())

  const url = buildMetricsUrl(filters)

  const fetchData = useCallback(async () => {
    try {
      const [m, l] = await Promise.all([
        apiFetch<DashboardMetrics>(url),
        apiFetch<ConsumerLagEntry[]>('/api/v1/consumer/lag'),
      ])
      setMetrics(m)
      setLag(l)
      setLastRefresh(new Date())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to fetch dashboard data')
    } finally {
      setLoading(false)
    }
  }, [url])

  useEffect(() => {
    fetchData()
    const id = setInterval(fetchData, 30000)
    return () => clearInterval(id)
  }, [fetchData])

  return { metrics, lag, loading, error, lastRefresh, refresh: fetchData }
}

export function useFilterOptions() {
  const [campaigns, setCampaigns] = useState<CampaignOption[]>([])
  const [fileNames, setFileNames] = useState<string[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const page = await apiFetch<{
          content: Array<{ id: number; campaignName: string }>
        }>('/api/v1/campaigns?page=0&size=100')

        const list = page.content.map((c) => ({ id: c.id, campaignName: c.campaignName }))
        setCampaigns(list)

        const details = await Promise.all(
          list.map((c) =>
            apiFetch<{ uploads: Array<{ fileName: string }> }>(`/api/v1/campaigns/${c.id}`)
              .catch(() => ({ uploads: [] }))
          )
        )
        const unique = [...new Set(details.flatMap((d) => d.uploads.map((u) => u.fileName)))]
        setFileNames(unique)
      } catch {
        // dropdowns are optional — silently degrade
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  return { campaigns, fileNames, loading }
}
