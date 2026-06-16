import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'
import { useCampaigns } from '@/hooks/useCampaigns'
import { apiFetch, hasApiKey } from '@/lib/api'

export const Route = createFileRoute('/send')({
  component: SendNotification,
})

interface SendResponse {
  eventId: number
  idempotencyKey: string
  status: string
  duplicate: boolean
}

function SendNotification() {
  const { data: campaignsPage, loading: campaignsLoading } = useCampaigns(0, 100)
  const campaigns = campaignsPage?.content ?? []

  const [campaignId, setCampaignId] = useState<number | ''>('')
  const [externalUserId, setExternalUserId] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState<SendResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!campaignId || !externalUserId.trim()) return
    setLoading(true)
    setError(null)
    try {
      const result = await apiFetch<SendResponse>('/api/v1/notifications/send', {
        method: 'POST',
        body: JSON.stringify({
          campaignId: Number(campaignId),
          externalUserId: externalUserId.trim(),
          ...(email.trim() && { email: email.trim() }),
          ...(phone.trim() && { phone: phone.trim() }),
        }),
      })
      setResponse(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send notification')
    } finally {
      setLoading(false)
    }
  }

  function handleReset() {
    setResponse(null)
    setError(null)
    setCampaignId('')
    setExternalUserId('')
    setEmail('')
    setPhone('')
  }

  const noKey = !hasApiKey()

  return (
    <div className="min-h-screen p-lg">
      <div className="max-w-[1200px] mx-auto">
        <div className="mb-xl">
          <h2 className="text-display font-bold text-on-background mb-sm">Send Notification</h2>
          <p className="text-on-surface-variant text-body-md">
            Trigger a single notification event across your delivery channels by providing the recipient's identifier and target campaign.
          </p>
        </div>

        {noKey && (
          <div className="mb-lg p-md bg-amber-500/10 border border-amber-500/20 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-amber-400 flex-shrink-0">key_off</span>
            <p className="text-body-sm text-on-surface">
              No API key configured.{' '}
              <Link to="/api-keys" className="text-secondary underline">Register a client</Link> first.
            </p>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-lg">
          {/* Form */}
          <div className="lg:col-span-7">
            <div className="glass-panel p-lg rounded-xl">
              <form className="space-y-lg" onSubmit={handleSubmit}>
                <div className="space-y-xs">
                  <label className="text-label-md text-on-surface-variant uppercase tracking-wider block">
                    Select Campaign
                  </label>
                  <div className="relative">
                    <select
                      className="w-full bg-surface-container-lowest border border-outline-variant focus:border-secondary text-on-surface p-md rounded-lg appearance-none transition-all outline-none disabled:opacity-50"
                      value={campaignId}
                      onChange={(e) => setCampaignId(Number(e.target.value))}
                      disabled={campaignsLoading}
                      required
                    >
                      <option value="" disabled>
                        {campaignsLoading ? 'Loading campaigns...' : campaigns.length === 0 ? 'No campaigns available' : 'Choose a campaign'}
                      </option>
                      {campaigns.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.campaignName} ({c.channel})
                        </option>
                      ))}
                    </select>
                    <span className="material-symbols-outlined absolute right-md top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">
                      expand_more
                    </span>
                  </div>
                </div>

                <div className="space-y-xs">
                  <label className="text-label-md text-on-surface-variant uppercase tracking-wider block">
                    External User ID <span className="text-error">*</span>
                  </label>
                  <input
                    className="w-full bg-surface-container-lowest border border-outline-variant focus:border-secondary text-on-surface p-md rounded-lg transition-all outline-none font-mono"
                    placeholder="e.g. user_9921_x"
                    value={externalUserId}
                    onChange={(e) => setExternalUserId(e.target.value)}
                    required
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                  <div className="space-y-xs">
                    <label className="text-label-md text-on-surface-variant uppercase tracking-wider block">
                      Email Address
                    </label>
                    <div className="relative">
                      <span className="material-symbols-outlined absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px]">mail</span>
                      <input
                        className="w-full bg-surface-container-lowest border border-outline-variant focus:border-secondary text-on-surface pl-10 pr-md py-md rounded-lg transition-all outline-none"
                        placeholder="jane@example.com"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="space-y-xs">
                    <label className="text-label-md text-on-surface-variant uppercase tracking-wider block">
                      Phone Number
                    </label>
                    <div className="relative">
                      <span className="material-symbols-outlined absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px]">phone_iphone</span>
                      <input
                        className="w-full bg-surface-container-lowest border border-outline-variant focus:border-secondary text-on-surface pl-10 pr-md py-md rounded-lg transition-all outline-none"
                        placeholder="+1 (555) 000-0000"
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                      />
                    </div>
                  </div>
                </div>

                {error && (
                  <div className="p-sm bg-error-container/20 border border-error/30 rounded-lg text-error text-body-sm">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading || noKey || !campaignId}
                  className="w-full bg-primary-container hover:bg-surface-variant text-on-background font-bold py-md rounded-xl border border-outline-variant transition-all active:scale-[0.98] flex items-center justify-center gap-sm disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                      </svg>
                      Processing...
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined">bolt</span>
                      Dispatch Notification
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Response Panel */}
          <div className="lg:col-span-5">
            {!response ? (
              <div className="h-full glass-panel border-dashed rounded-xl flex flex-col items-center justify-center p-xl text-center min-h-[320px]">
                <div className="w-16 h-16 rounded-full bg-surface-variant/50 flex items-center justify-center mb-md">
                  <span className="material-symbols-outlined text-outline text-[32px]">code_off</span>
                </div>
                <p className="text-on-surface-variant text-label-md uppercase tracking-widest mb-sm">Awaiting Trigger</p>
                <p className="text-on-surface-variant/60 text-body-sm px-lg">
                  Fill out the form to the left and dispatch to see the API response here.
                </p>
              </div>
            ) : (
              <div className="glass-panel border-secondary-container/50 bg-secondary-container/5 rounded-xl flex flex-col p-lg h-full">
                <div className="flex items-center justify-between mb-lg">
                  <div className="flex items-center gap-sm">
                    <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center">
                      <span className="material-symbols-outlined text-on-secondary-container">check_circle</span>
                    </div>
                    <div>
                      <h3 className="text-headline-md font-bold text-on-background">Queued</h3>
                      <p className="text-on-surface-variant text-body-sm">Notification dispatched successfully</p>
                    </div>
                  </div>
                  <span className="px-sm py-xs bg-secondary-container/20 text-on-secondary-container rounded-lg text-label-sm border border-secondary-container/30">
                    STATUS: 202
                  </span>
                </div>

                <div className="space-y-md flex-1">
                  {[
                    { label: 'Event ID', value: String(response.eventId), color: 'text-secondary' },
                    { label: 'Idempotency Key', value: response.idempotencyKey, color: 'text-on-surface' },
                  ].map((item) => (
                    <div key={item.label} className="p-md bg-surface-container-lowest/50 border border-outline-variant/30 rounded-lg">
                      <div className="flex items-center justify-between mb-xs">
                        <span className="text-label-sm text-on-surface-variant uppercase tracking-tighter">{item.label}</span>
                        <button
                          className="material-symbols-outlined text-outline text-[16px] hover:text-on-surface"
                          onClick={() => navigator.clipboard.writeText(item.value)}
                        >
                          content_copy
                        </button>
                      </div>
                      <p className={`font-mono text-body-sm ${item.color}`}>{item.value}</p>
                    </div>
                  ))}

                  <div className="flex gap-md">
                    <div className="flex-1 p-md bg-surface-container-lowest/50 border border-outline-variant/30 rounded-lg">
                      <span className="text-label-sm text-on-surface-variant uppercase tracking-tighter block mb-xs">Duplicate</span>
                      <span className={`font-bold text-body-md ${response.duplicate ? 'text-amber-400' : 'text-secondary'}`}>
                        {response.duplicate ? 'TRUE' : 'FALSE'}
                      </span>
                    </div>
                    <div className="flex-1 p-md bg-surface-container-lowest/50 border border-outline-variant/30 rounded-lg">
                      <span className="text-label-sm text-on-surface-variant uppercase tracking-tighter block mb-xs">Status</span>
                      <span className="font-bold text-on-surface text-body-md">{response.status}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-lg pt-lg border-t border-outline-variant/30 flex justify-end">
                  <button className="text-secondary text-label-md hover:underline" onClick={handleReset}>
                    New Request
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
