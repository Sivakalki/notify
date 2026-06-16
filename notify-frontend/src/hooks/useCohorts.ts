import { useState, useEffect, useCallback } from 'react'
import { apiFetch } from '@/lib/api'

export interface Cohort {
  id: number
  name: string
  description: string
  memberCount: number
  createdAt: string
}

export interface AddUsersResult {
  total: number
  added: number
  duplicates: number
}

export function useCohorts() {
  const [data, setData] = useState<Cohort[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const result = await apiFetch<Cohort[]>('/api/v1/cohorts')
      setData(result)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load cohorts')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  async function createCohort(body: { name: string; description: string }): Promise<Cohort> {
    const result = await apiFetch<Cohort>('/api/v1/cohorts', {
      method: 'POST',
      body: JSON.stringify(body),
    })
    await fetchData()
    return result
  }

  async function addUsers(
    cohortId: number,
    users: Array<{ externalUserId: string; email?: string; phone?: string }>,
  ): Promise<AddUsersResult> {
    return apiFetch<AddUsersResult>(`/api/v1/cohorts/${cohortId}/users`, {
      method: 'POST',
      body: JSON.stringify({ users }),
    })
  }

  async function removeUser(cohortId: number, userId: number): Promise<void> {
    await apiFetch<void>(`/api/v1/cohorts/${cohortId}/users/${userId}`, {
      method: 'DELETE',
    })
    await fetchData()
  }

  return { data, loading, error, createCohort, addUsers, removeUser, refresh: fetchData }
}
