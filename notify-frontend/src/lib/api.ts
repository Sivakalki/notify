export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const CLIENT_UUID = import.meta.env.VITE_CLIENT_UUID

export const apiKeyStorage = {
  get: (): string | null => localStorage.getItem('notify_api_key'),
  set: (key: string): void => { localStorage.setItem('notify_api_key', key) },
  getClientId: (): string | null => localStorage.getItem('notify_client_id'),
  setClientId: (id: string): void => { localStorage.setItem('notify_client_id', id) },
}

export function hasApiKey(): boolean {
  return !!apiKeyStorage.get()
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  }

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  if (CLIENT_UUID) headers['X-Client-UUID'] = CLIENT_UUID

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error((body as { message?: string }).message ?? `HTTP ${res.status}`)
  }

  if (res.status === 204) return undefined as T

  const ct = res.headers.get('content-type') ?? ''
  if (ct.includes('application/json')) return res.json() as Promise<T>
  return res.text() as unknown as T
}
