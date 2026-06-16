import { createFileRoute } from '@tanstack/react-router'
import { useState } from 'react'
import { useCampaigns, useNotificationHistory } from '@/hooks/useCampaigns'
import type { Campaign } from '@/hooks/useCampaigns'

export const Route = createFileRoute('/campaigns')({
  component: Campaigns,
})

const channelBadge: Record<string, string> = {
  EMAIL: 'bg-secondary/10 text-secondary border-secondary/20',
  SMS: 'bg-amber-400/10 text-amber-400 border-amber-400/20',
  IN_APP: 'bg-primary/10 text-primary border-primary/20',
}

const statusStyle: Record<string, { dot: string; text: string }> = {
  SENT: { dot: 'bg-emerald-400', text: 'text-emerald-400' },
  FAILED: { dot: 'bg-red-400 animate-pulse', text: 'text-red-400' },
  CANCELLED: { dot: 'bg-red-400', text: 'text-red-400' },
  PENDING: { dot: 'bg-amber-400', text: 'text-amber-400' },
  PROCESSING: { dot: 'bg-blue-400 animate-pulse', text: 'text-blue-400' },
}

function getStatus(s: string) {
  return statusStyle[s] ?? { dot: 'bg-outline', text: 'text-on-surface-variant' }
}

function successRate(c: Campaign) {
  const total = c.sentCount + c.failedCount
  return total > 0 ? Math.round((c.sentCount / total) * 100) : 0
}

function Campaigns() {
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState('')
  const [slideOpen, setSlideOpen] = useState(false)
  const [selectedCampaign, setSelectedCampaign] = useState<Campaign | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const [formName, setFormName] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const [formChannel, setFormChannel] = useState<'EMAIL' | 'SMS' | 'IN_APP'>('EMAIL')

  const { data, loading, error, createCampaign, deleteCampaign } = useCampaigns(page)
  const { data: history, loading: histLoading } = useNotificationHistory(selectedCampaign?.id ?? null)

  const campaigns = data?.content ?? []
  const filtered = filter
    ? campaigns.filter((c) => c.campaignName.toLowerCase().includes(filter.toLowerCase()))
    : campaigns

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!formName.trim() || !formMessage.trim()) return
    setSubmitting(true)
    setFormError(null)
    try {
      await createCampaign({ campaignName: formName, message: formMessage, channel: formChannel })
      setSlideOpen(false)
      setFormName('')
      setFormMessage('')
      setFormChannel('EMAIL')
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to create campaign')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: number, e: React.MouseEvent) {
    e.stopPropagation()
    if (deleteId !== id) { setDeleteId(id); return }
    try {
      await deleteCampaign(id)
      if (selectedCampaign?.id === id) setSelectedCampaign(null)
    } catch { /* ignore */ }
    setDeleteId(null)
  }

  return (
    <div className="p-lg">
      <div className="max-w-[1600px] mx-auto space-y-lg">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-headline-lg font-bold text-on-surface">Campaigns</h1>
            <p className="text-on-surface-variant text-body-sm">Manage and monitor high-frequency notification flows.</p>
          </div>
          <button
            className="flex items-center gap-sm bg-primary text-on-primary px-lg py-sm rounded-lg text-label-md hover:opacity-90 transition-opacity"
            onClick={() => setSlideOpen(true)}
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            New Campaign
          </button>
        </div>

        {error && (
          <div className="p-md bg-error-container/20 border border-error/30 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-error">warning</span>
            <span className="text-body-sm text-on-surface">{error}</span>
          </div>
        )}

        {/* Table */}
        <div className="bg-surface-container border border-outline-variant rounded-xl overflow-hidden">
          <div className="p-md border-b border-outline-variant flex items-center justify-between bg-surface-container-high/50">
            <div className="flex items-center gap-md">
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
                <input
                  className="bg-surface-container-lowest border border-outline-variant rounded-lg pl-10 pr-md py-1.5 text-body-sm w-64 focus:border-secondary outline-none transition-colors"
                  placeholder="Filter campaigns..."
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                />
              </div>
            </div>
            <div className="text-on-surface-variant text-label-sm">
              {data ? `${data.totalElements} campaigns` : '—'}
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-surface-container-high text-on-surface-variant text-label-sm uppercase tracking-wider">
                  {['Name', 'Channel', 'Status', 'Performance', 'Created', 'Actions'].map((h, i) => (
                    <th key={h} className={`px-md py-3 border-b border-outline-variant font-semibold ${i === 5 ? 'text-center' : ''}`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {loading
                  ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 6 }).map((__, j) => (
                        <td key={j} className="px-md py-4">
                          <div className="h-4 bg-surface-variant rounded animate-pulse" />
                        </td>
                      ))}
                    </tr>
                  ))
                  : filtered.length === 0
                    ? (
                      <tr>
                        <td colSpan={6} className="px-md py-xl text-center text-on-surface-variant text-body-sm">
                          No campaigns found.
                        </td>
                      </tr>
                    )
                    : filtered.map((c) => {
                      const ss = getStatus(c.status)
                      const rate = successRate(c)
                      return (
                        <tr
                          key={c.id}
                          className={`hover:bg-surface-container-highest/30 transition-colors cursor-pointer ${selectedCampaign?.id === c.id ? 'bg-surface-variant/20' : ''}`}
                          onClick={() => setSelectedCampaign(selectedCampaign?.id === c.id ? null : c)}
                        >
                          <td className="px-md py-4">
                            <div className="text-body-md text-on-surface font-medium">{c.campaignName}</div>
                            <div className="text-xs text-on-surface-variant font-mono">ID: {c.id}</div>
                          </td>
                          <td className="px-md py-4">
                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold border ${channelBadge[c.channel] ?? channelBadge.EMAIL}`}>
                              {c.channel}
                            </span>
                          </td>
                          <td className="px-md py-4">
                            <span className={`inline-flex items-center gap-1.5 ${ss.text} text-label-sm`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${ss.dot}`} />
                              {c.status}
                            </span>
                          </td>
                          <td className="px-md py-4">
                            <div className="flex flex-col items-end gap-1">
                              <div className="text-on-surface font-mono text-xs">
                                {c.sentCount + c.failedCount > 0 ? `${rate}% success` : '—'}
                              </div>
                              <div className="w-32 h-1 bg-outline-variant rounded-full overflow-hidden">
                                <div className="h-full bg-emerald-400" style={{ width: `${rate}%` }} />
                              </div>
                              <div className="text-[10px] text-on-surface-variant">
                                {c.sentCount.toLocaleString()} sent / {c.failedCount.toLocaleString()} failed
                              </div>
                            </div>
                          </td>
                          <td className="px-md py-4 text-on-surface-variant text-body-sm">
                            {new Date(c.createdAt).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })}
                          </td>
                          <td className="px-md py-4 text-center" onClick={(e) => e.stopPropagation()}>
                            <button
                              className={`px-sm py-0.5 rounded text-label-sm border transition-colors ${deleteId === c.id ? 'bg-error-container text-on-error-container border-error' : 'text-error border-error/30 hover:bg-error/10'}`}
                              onClick={(e) => handleDelete(c.id, e)}
                            >
                              {deleteId === c.id ? 'Confirm?' : 'Delete'}
                            </button>
                          </td>
                        </tr>
                      )
                    })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="p-md bg-surface-container-low flex items-center justify-between border-t border-outline-variant">
              <span className="text-label-sm text-on-surface-variant">
                Page {page + 1} of {data.totalPages}
              </span>
              <div className="flex gap-1">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="p-1 hover:bg-surface-variant rounded disabled:opacity-30"
                >
                  <span className="material-symbols-outlined text-[20px]">chevron_left</span>
                </button>
                <button
                  disabled={page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-1 hover:bg-surface-variant rounded disabled:opacity-30"
                >
                  <span className="material-symbols-outlined text-[20px]">chevron_right</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Campaign Detail */}
        {selectedCampaign && (
          <div className="border-t border-outline-variant pt-lg">
            <div className="flex items-center justify-between mb-lg">
              <div className="flex items-center gap-md">
                <button className="p-2 hover:bg-surface-variant rounded-full" onClick={() => setSelectedCampaign(null)}>
                  <span className="material-symbols-outlined">arrow_back</span>
                </button>
                <div>
                  <h2 className="text-headline-md font-bold text-on-surface">{selectedCampaign.campaignName}</h2>
                  <p className="text-on-surface-variant text-label-sm">Campaign ID: {selectedCampaign.id}</p>
                </div>
              </div>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-md mb-lg">
              {[
                { label: 'Sent', value: selectedCampaign.sentCount, color: 'text-emerald-400', icon: 'check_circle', iconColor: 'text-emerald-400' },
                { label: 'Failed', value: selectedCampaign.failedCount, color: 'text-error', icon: 'error', iconColor: 'text-error' },
                { label: 'Pending', value: selectedCampaign.totalUsers - selectedCampaign.sentCount - selectedCampaign.failedCount, color: 'text-on-surface-variant', icon: 'schedule', iconColor: 'text-amber-400' },
              ].map((s) => (
                <div key={s.label} className="bg-surface-container-high border border-outline-variant p-lg rounded-xl">
                  <div className="flex items-center justify-between mb-sm">
                    <span className="text-on-surface-variant text-label-md">{s.label}</span>
                    <span className={`material-symbols-outlined ${s.iconColor}`}>{s.icon}</span>
                  </div>
                  <div className={`text-display font-bold ${s.color}`}>{Math.max(0, s.value).toLocaleString()}</div>
                </div>
              ))}
            </div>

            {/* Notification History */}
            <div className="bg-surface-container border border-outline-variant rounded-xl overflow-hidden">
              <div className="p-md border-b border-outline-variant text-label-md font-bold text-on-surface">
                Notification History {history ? `(${history.totalElements})` : ''}
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead className="bg-surface-container-high text-[11px] uppercase text-on-surface-variant">
                    <tr>
                      {['Event ID', 'User ID', 'Channel', 'Status', 'Created At', 'Delivered At', 'Error'].map((h) => (
                        <th key={h} className="px-md py-3 font-semibold border-b border-outline-variant">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant font-mono text-xs">
                    {histLoading
                      ? Array.from({ length: 3 }).map((_, i) => (
                        <tr key={i}>
                          {Array.from({ length: 7 }).map((__, j) => (
                            <td key={j} className="px-md py-3">
                              <div className="h-3 bg-surface-variant rounded animate-pulse" />
                            </td>
                          ))}
                        </tr>
                      ))
                      : (history?.content ?? []).length === 0
                        ? (
                          <tr>
                            <td colSpan={7} className="px-md py-xl text-center text-on-surface-variant not-italic">
                              No notification history yet.
                            </td>
                          </tr>
                        )
                        : (history?.content ?? []).map((row) => {
                          const ss = getStatus(row.status)
                          return (
                            <tr key={row.eventId} className="hover:bg-surface-variant/20">
                              <td className="px-md py-3 text-on-surface">{row.eventId}</td>
                              <td className="px-md py-3 text-on-surface-variant">{row.externalUserId}</td>
                              <td className="px-md py-3 text-on-surface-variant">{row.channel}</td>
                              <td className="px-md py-3"><span className={ss.text}>{row.status}</span></td>
                              <td className="px-md py-3 text-on-surface-variant">{row.createdAt}</td>
                              <td className="px-md py-3 text-on-surface-variant">{row.deliveredAt ?? '—'}</td>
                              <td className={`px-md py-3 text-[10px] ${row.errorMessage ? 'text-error' : 'text-outline'}`}>
                                {row.errorMessage ?? '—'}
                              </td>
                            </tr>
                          )
                        })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* New Campaign Slide-over */}
      {slideOpen && (
        <>
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50" onClick={() => setSlideOpen(false)} />
          <div className="fixed top-0 right-0 h-full w-[450px] bg-surface-container-high border-l border-outline-variant z-[60] flex flex-col">
            <div className="p-lg border-b border-outline-variant flex items-center justify-between">
              <div>
                <h3 className="text-headline-md font-bold text-on-surface">New Campaign</h3>
                <p className="text-on-surface-variant text-label-sm">Create a single or multi-channel broadcast.</p>
              </div>
              <button className="p-2 hover:bg-surface-variant rounded-full text-on-surface-variant" onClick={() => setSlideOpen(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form className="p-lg space-y-lg flex-1 overflow-y-auto" onSubmit={handleCreate}>
              <div className="space-y-xs">
                <label className="text-label-md text-on-surface">Campaign Name</label>
                <input
                  className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-body-md focus:border-primary outline-none"
                  placeholder="e.g. Black Friday Launch"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-xs">
                <label className="text-label-md text-on-surface">Channel</label>
                <div className="grid grid-cols-3 gap-sm">
                  {(['EMAIL', 'SMS', 'IN_APP'] as const).map((ch) => (
                    <button
                      key={ch}
                      type="button"
                      className={`flex flex-col items-center gap-2 p-md border-2 rounded-xl transition-colors ${formChannel === ch ? 'border-primary bg-primary/5' : 'border-outline-variant hover:border-primary/50'}`}
                      onClick={() => setFormChannel(ch)}
                    >
                      <span className="material-symbols-outlined">
                        {ch === 'EMAIL' ? 'mail' : ch === 'SMS' ? 'sms' : 'notifications_active'}
                      </span>
                      <span className="text-label-sm">{ch === 'IN_APP' ? 'In-App' : ch}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-xs">
                <label className="text-label-md text-on-surface">Message</label>
                <textarea
                  className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-body-md focus:border-primary outline-none resize-none"
                  placeholder="Write your message here..."
                  rows={6}
                  value={formMessage}
                  onChange={(e) => setFormMessage(e.target.value)}
                  required
                />
              </div>

              {formError && (
                <div className="p-sm bg-error-container/20 border border-error/30 rounded-lg text-error text-body-sm">
                  {formError}
                </div>
              )}
            </form>

            <div className="p-lg border-t border-outline-variant flex gap-md">
              <button
                className="flex-1 bg-primary text-on-primary py-3 rounded-lg text-label-md font-bold hover:opacity-90 disabled:opacity-60"
                onClick={handleCreate}
                disabled={submitting}
              >
                {submitting ? 'Creating...' : 'Launch Campaign'}
              </button>
              <button
                className="flex-1 border border-outline-variant py-3 rounded-lg text-label-md text-on-surface hover:bg-surface-variant transition-colors"
                type="button"
                onClick={() => setSlideOpen(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
