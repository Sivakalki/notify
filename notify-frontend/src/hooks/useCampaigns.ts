import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '@/lib/api'

export interface Campaign {
  id: number
  campaignName: string
  message: string
  channel: 'EMAIL' | 'SMS' | 'IN_APP'
  status: string
  totalUsers: number
  sentCount: number
  failedCount: number
  duplicateCount: number
  createdAt: string
  updatedAt: string
}

export interface BulkUploadEntry {
  uploadId: number
  campaignId: number
  fileName: string
  fileType: string
  rowCount: number
  duplicateCount: number
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}

export interface CampaignDetail extends Campaign {
  uploads: BulkUploadEntry[]
}

export interface NotificationEvent {
  eventId: number
  externalUserId: string
  channel: string
  status: string
  createdAt: string
  deliveredAt: string | null
  errorMessage: string | null
}

interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export function useCampaigns(page = 0, size = 20) {
  const [data, setData] = useState<Page<Campaign> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const result = await apiFetch<Page<Campaign>>(`/api/v1/campaigns?page=${page}&size=${size}`)
      setData(result)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load campaigns')
    } finally {
      setLoading(false)
    }
  }, [page, size])

  useEffect(() => { fetchData() }, [fetchData])

  async function createCampaign(body: { campaignName: string; message: string; channel: string }): Promise<Campaign> {
    const result = await apiFetch<Campaign>('/api/v1/campaigns', {
      method: 'POST',
      body: JSON.stringify(body),
    })
    await fetchData()
    return result
  }

  async function updateCampaign(
    id: number,
    body: { campaignName: string; message: string; channel: string; status: string },
  ): Promise<Campaign> {
    const result = await apiFetch<Campaign>(`/api/v1/campaigns/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    await fetchData()
    return result
  }

  async function getCampaign(id: number): Promise<CampaignDetail> {
    return apiFetch<CampaignDetail>(`/api/v1/campaigns/${id}`)
  }

  async function deleteCampaign(id: number): Promise<void> {
    await apiFetch<void>(`/api/v1/campaigns/${id}`, { method: 'DELETE' })
    await fetchData()
  }

  return { data, loading, error, createCampaign, updateCampaign, getCampaign, deleteCampaign, refresh: fetchData }
}

export function useNotificationHistory(campaignId: number | null) {
  const [data, setData] = useState<Page<NotificationEvent> | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (campaignId === null) { setData(null); return }
    setLoading(true)
    apiFetch<Page<NotificationEvent>>(
      `/api/v1/notifications/history?campaignId=${campaignId}&page=0&size=50`
    )
      .then(setData)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [campaignId])

  return { data, loading }
}
