import { createFileRoute } from '@tanstack/react-router'
import { useState } from 'react'
import { apiFetch, apiKeyStorage } from '@/lib/api'

export const Route = createFileRoute('/api-keys')({
  component: ApiKeys,
})

interface RegisterResponse {
  clientId: string
  apiKey: string
}

const docLinks = [
  { icon: 'menu_book', label: 'Authentication Basics', sub: 'Learn how to sign requests with your key.' },
  { icon: 'api', label: 'REST Endpoints', sub: 'Browse available resources and methods.' },
  { icon: 'security', label: 'Scoped Permissions', sub: 'Limit access levels for sub-clients.' },
]

function ApiKeys() {
  const [clientName, setClientName] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<RegisterResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null)
  const [copied, setCopied] = useState(false)

  const existingKey = apiKeyStorage.get()
  const existingClientId = apiKeyStorage.getClientId()

  async function handleRegister() {
    if (!clientName.trim()) {
      showToast('Please enter a client name', 'error')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await apiFetch<RegisterResponse>('/api/v1/clients/register', {
        method: 'POST',
        body: JSON.stringify({ name: clientName.trim() }),
      })
      apiKeyStorage.set(data.apiKey)
      apiKeyStorage.setClientId(data.clientId)
      setResult(data)
      showToast('API Key generated successfully', 'success')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Registration failed'
      setError(msg)
      showToast(msg, 'error')
    } finally {
      setLoading(false)
    }
  }

  function showToast(msg: string, type: 'success' | 'error') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3000)
  }

  function copyKey(key: string) {
    navigator.clipboard.writeText(key).then(() => {
      setCopied(true)
      showToast('API Key copied to clipboard', 'success')
      setTimeout(() => setCopied(false), 2000)
    })
  }

  const registered = !!result

  return (
    <div className="p-xl">
      <div className="max-w-4xl mx-auto">
        <header className="mb-xl">
          <h2 className="text-display font-display text-on-surface mb-2">API Key Management</h2>
          <p className="text-body-lg text-on-surface-variant">
            Register your client and generate secure access tokens for the Notify API gateway.
          </p>
        </header>

        {existingKey && !registered && (
          <div className="mb-lg p-md bg-secondary-container/10 border border-secondary-container/30 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-secondary flex-shrink-0">key</span>
            <div className="flex-1 min-w-0">
              <p className="text-label-md text-on-surface font-bold">Active API Key</p>
              <p className="text-body-sm text-on-surface-variant font-mono truncate">Client: {existingClientId ?? '—'}</p>
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[10px] font-bold border border-emerald-500/20 uppercase">
              Active
            </span>
          </div>
        )}

        <div className="grid grid-cols-12 gap-lg">
          {/* Registration Form */}
          {!registered && (
            <div className="col-span-12 lg:col-span-7" id="registration-form">
              <div className="glass-panel p-lg rounded-xl flex flex-col gap-lg">
                <div className="flex items-center gap-sm">
                  <span className="material-symbols-outlined text-primary">app_registration</span>
                  <h3 className="text-headline-md font-headline-md text-on-surface">Client Registration</h3>
                </div>
                <div className="space-y-sm">
                  <label className="block text-label-md text-on-surface-variant uppercase tracking-wider">
                    Client Name
                  </label>
                  <input
                    className="w-full bg-surface-container-lowest border border-outline-variant text-on-surface p-md rounded focus:ring-1 focus:ring-secondary focus:border-secondary outline-none transition-all placeholder:text-outline"
                    placeholder="e.g. Production Mobile App"
                    value={clientName}
                    onChange={(e) => setClientName(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleRegister()}
                  />
                  <p className="text-label-sm text-outline">
                    This name will be used to identify your API usage in the dashboard.
                  </p>
                </div>

                {error && (
                  <div className="p-sm bg-error-container/20 border border-error/30 rounded-lg text-error text-body-sm">
                    {error}
                  </div>
                )}

                <button
                  className="w-full py-md bg-primary-container text-on-primary font-bold rounded flex items-center justify-center gap-sm hover:brightness-110 active:opacity-80 transition-all border border-outline-variant disabled:opacity-60"
                  onClick={handleRegister}
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="material-symbols-outlined animate-spin text-sm">progress_activity</span>
                      Processing...
                    </>
                  ) : (
                    <>
                      <span>Register &amp; Generate Key</span>
                      <span className="material-symbols-outlined text-sm">arrow_forward</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* Success State */}
          {registered && result && (
            <div className="col-span-12">
              <div className="glass-panel p-lg rounded-xl border-2 border-secondary-container relative overflow-hidden">
                <div className="relative z-10">
                  <div className="flex items-center justify-between mb-lg">
                    <div className="flex items-center gap-sm">
                      <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center">
                        <span className="material-symbols-outlined text-on-secondary-container">check_circle</span>
                      </div>
                      <div>
                        <h3 className="text-headline-md font-headline-md text-on-surface">Client Successfully Registered</h3>
                        <p className="text-label-md text-secondary">
                          Client ID: <span className="font-mono">{result.clientId}</span>
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-label-sm text-on-surface-variant uppercase">Created</p>
                      <p className="text-label-md text-on-surface">{new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })}</p>
                    </div>
                  </div>

                  <div className="bg-surface-container-lowest border border-outline-variant p-lg rounded mb-lg">
                    <label className="block text-label-md text-on-surface-variant mb-sm uppercase tracking-wider">Secret API Key</label>
                    <div className="flex gap-sm">
                      <div className="flex-1 font-mono text-lg bg-surface-container-highest p-md rounded border border-outline-variant text-on-surface break-all flex items-center">
                        {result.apiKey}
                      </div>
                      <button
                        className="bg-surface-variant px-lg rounded border border-outline-variant text-on-surface hover:bg-primary-container transition-colors flex items-center gap-xs text-label-md"
                        onClick={() => copyKey(result.apiKey)}
                      >
                        <span className="material-symbols-outlined">{copied ? 'check' : 'content_copy'}</span>
                        <span className="font-bold uppercase">{copied ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  </div>

                  <div className="bg-error-container/20 border border-error-container p-md rounded-lg flex items-start gap-md">
                    <span className="material-symbols-outlined text-error">warning</span>
                    <div>
                      <p className="font-bold text-error mb-1">Security Warning</p>
                      <p className="text-body-sm text-on-error-container">
                        This key will not be shown again. Store it securely in your secrets manager. If lost, you will need to revoke this client and generate a new one.
                      </p>
                    </div>
                  </div>

                  <div className="mt-lg">
                    <button
                      className="text-secondary text-label-md hover:underline"
                      onClick={() => { setResult(null); setClientName('') }}
                    >
                      Register another client
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Side Info */}
          {!registered && (
            <div className="col-span-12 lg:col-span-5 flex flex-col gap-lg">
              <div className="bg-surface-container p-lg rounded-xl border border-outline-variant">
                <h4 className="text-label-md font-label-md font-bold text-on-surface uppercase mb-md tracking-widest">Integration Guide</h4>
                <ul className="space-y-md">
                  {docLinks.map((link) => (
                    <li key={link.label} className="flex gap-sm">
                      <span className="material-symbols-outlined text-primary text-[20px]">{link.icon}</span>
                      <div>
                        <p className="text-label-md text-on-surface">{link.label}</p>
                        <p className="text-label-sm text-on-surface-variant">{link.sub}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="relative rounded-xl h-48 overflow-hidden border border-outline-variant bg-surface-container-high flex items-end p-lg">
                <p className="text-label-md text-on-surface font-bold">Enterprise Security Standard</p>
              </div>
            </div>
          )}
        </div>

        {/* Active Key Info */}
        {existingKey && (
          <section className="mt-xl">
            <div className="flex items-center justify-between mb-md">
              <h3 className="text-label-md font-label-md font-bold text-on-surface uppercase tracking-widest">Current Session</h3>
            </div>
            <div className="glass-panel rounded-xl overflow-hidden">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-surface-container-high border-b border-outline-variant">
                    {['Field', 'Value', 'Status'].map((h) => (
                      <th key={h} className="p-md text-label-sm text-on-surface-variant uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-b border-outline-variant hover:bg-surface-variant transition-colors">
                    <td className="p-md text-body-md text-on-surface">Client ID</td>
                    <td className="p-md font-mono text-xs text-on-surface-variant">{existingClientId ?? '—'}</td>
                    <td className="p-md">
                      <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[10px] font-bold border border-emerald-500/20 uppercase tracking-tighter">
                        Active
                      </span>
                    </td>
                  </tr>
                  <tr className="hover:bg-surface-variant transition-colors">
                    <td className="p-md text-body-md text-on-surface">API Key</td>
                    <td className="p-md font-mono text-xs text-on-surface-variant">
                      {existingKey.slice(0, 8)}{'•'.repeat(12)}{existingKey.slice(-4)}
                    </td>
                    <td className="p-md">
                      <button
                        className="text-label-sm text-secondary hover:underline"
                        onClick={() => copyKey(existingKey)}
                      >
                        Copy
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        )}
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-lg right-lg z-50 flex flex-col gap-sm">
          <div className="flex items-center gap-sm bg-surface-container-high border border-outline-variant p-md rounded-lg shadow-2xl min-w-[300px]">
            <div className={`w-1 h-8 rounded-full ${toast.type === 'success' ? 'bg-secondary-container' : 'bg-error-container'}`} />
            <span className={`material-symbols-outlined ${toast.type === 'success' ? 'text-secondary' : 'text-error'}`}>
              {toast.type === 'success' ? 'check_circle' : 'error'}
            </span>
            <span className="text-label-md text-on-surface">{toast.msg}</span>
          </div>
        </div>
      )}
    </div>
  )
}
