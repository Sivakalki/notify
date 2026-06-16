import { createFileRoute } from '@tanstack/react-router'
import { useState } from 'react'
import { useDLQ } from '@/hooks/useDLQ'
import type { DLQEntry } from '@/hooks/useDLQ'

export const Route = createFileRoute('/dlq')({
  component: DLQManager,
})

type RowState = 'idle' | 'confirm' | 'replaying'

function DLQManager() {
  const [page, setPage] = useState(0)
  const { data, loading, error, reprocess, refresh } = useDLQ(page)

  const [rowStates, setRowStates] = useState<Record<number, RowState>>({})
  const [replayError, setReplayError] = useState<string | null>(null)

  async function handleReplay(entry: DLQEntry) {
    const current = rowStates[entry.id] ?? 'idle'

    if (current === 'idle') {
      setRowStates((s) => ({ ...s, [entry.id]: 'confirm' }))
      setTimeout(() => {
        setRowStates((s) => s[entry.id] === 'confirm' ? { ...s, [entry.id]: 'idle' } : s)
      }, 3000)
      return
    }

    if (current === 'confirm') {
      setRowStates((s) => ({ ...s, [entry.id]: 'replaying' }))
      setReplayError(null)
      try {
        await reprocess(entry.id)
      } catch (err) {
        setReplayError(err instanceof Error ? err.message : 'Reprocess failed')
        setRowStates((s) => ({ ...s, [entry.id]: 'idle' }))
      }
    }
  }

  const rows = data?.content ?? []

  return (
    <div className="p-lg min-h-screen">
      <div className="max-w-[1600px] mx-auto space-y-lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-md">
          <div>
            <div className="flex items-center gap-sm mb-1">
              <span className="px-2 py-0.5 rounded bg-secondary-container text-on-secondary-container font-mono text-[10px] tracking-widest uppercase">Manager</span>
              <h2 className="text-headline-md font-headline-md">Dead Letter Queue</h2>
            </div>
            <p className="text-body-sm text-on-surface-variant">
              Manage and replay failed notification events.{' '}
              {data && (
                <span className="text-secondary font-bold">{data.totalElements} event{data.totalElements !== 1 ? 's' : ''} pending</span>
              )}
            </p>
          </div>
          <button
            className="flex items-center gap-sm bg-surface-container-high border border-outline-variant px-md py-sm rounded-lg hover:bg-surface-variant transition-colors text-on-surface text-label-md"
            onClick={refresh}
          >
            <span className="material-symbols-outlined text-[18px]">refresh</span>
            Refresh
          </button>
        </div>

        {error && (
          <div className="p-md bg-error-container/20 border border-error/30 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-error">warning</span>
            <span className="text-body-sm text-on-surface">{error}</span>
          </div>
        )}

        {replayError && (
          <div className="p-md bg-error-container/20 border border-error/30 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-error">error</span>
            <span className="text-body-sm text-on-surface">{replayError}</span>
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-md">
          <div className="bg-surface-container p-md rounded-xl border border-outline-variant">
            <p className="text-label-sm font-mono text-on-surface-variant mb-1 uppercase tracking-tighter">Queue Depth</p>
            <div className="flex items-baseline gap-sm">
              <span className="text-display font-display text-secondary">
                {loading ? '—' : (data?.totalElements ?? 0).toLocaleString()}
              </span>
            </div>
          </div>
          <div className="bg-surface-container p-md rounded-xl border border-outline-variant">
            <p className="text-label-sm font-mono text-on-surface-variant mb-1 uppercase tracking-tighter">Success Rate (Replay)</p>
            <span className="text-display font-display text-on-surface">94.2%</span>
          </div>
          <div className="bg-surface-container p-md rounded-xl border border-outline-variant">
            <p className="text-label-sm font-mono text-on-surface-variant mb-1 uppercase tracking-tighter">Avg TTL in DLQ</p>
            <span className="text-display font-display text-on-surface">4.2h</span>
          </div>
          <div className="bg-surface-container p-md rounded-xl border border-outline-variant relative overflow-hidden">
            <p className="text-label-sm font-mono text-on-surface-variant mb-1 uppercase tracking-tighter">Health Monitor</p>
            <div className="h-10 mt-2 flex items-end gap-[2px]">
              {[4, 6, 5, 9, 7, 3, 10].map((h, i) => (
                <div
                  key={i}
                  className="w-full bg-secondary rounded-t-sm"
                  style={{ height: `${h * 4}px`, opacity: 0.2 + (h / 10) * 0.8 }}
                />
              ))}
            </div>
          </div>
        </div>

        {/* DLQ Table */}
        <div className="bg-surface-container-low border border-outline-variant rounded-xl overflow-hidden shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-container-high border-b border-outline-variant">
                  {['ID', 'Event ID', 'Reason', 'Payload Preview', 'Created Date', 'Actions'].map((h, i) => (
                    <th key={h} className={`p-md text-label-sm uppercase tracking-widest text-on-surface-variant ${i === 5 ? 'text-right' : ''}`}>
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
                          <td key={j} className="p-md">
                            <div className="h-4 bg-surface-variant rounded animate-pulse" />
                          </td>
                        ))}
                      </tr>
                    ))
                  : rows.length === 0
                    ? (
                      <tr>
                        <td colSpan={6} className="p-md py-xl text-center text-on-surface-variant text-body-sm">
                          No events in the dead letter queue.
                        </td>
                      </tr>
                    )
                    : rows.map((row) => {
                        const state = rowStates[row.id] ?? 'idle'
                        return (
                          <tr key={row.id} className="hover:bg-surface-container-high transition-colors">
                            <td className="p-md font-mono text-label-md text-on-surface">#{row.id}</td>
                            <td className="p-md font-mono text-label-sm text-secondary">{row.eventId}</td>
                            <td className="p-md">
                              <span className="px-2 py-1 rounded border text-label-sm bg-error-container/20 text-error border-error/20">
                                {row.reason}
                              </span>
                            </td>
                            <td className="p-md">
                              <div className="max-w-[200px] truncate font-mono text-label-sm text-on-surface-variant bg-surface-container-lowest p-1 rounded">
                                {row.payload}
                              </div>
                            </td>
                            <td className="p-md text-label-sm text-on-surface-variant">
                              {new Date(row.createdAt).toLocaleString()}
                            </td>
                            <td className="p-md text-right">
                              {state === 'replaying' ? (
                                <span className="material-symbols-outlined text-[14px] animate-spin text-primary">refresh</span>
                              ) : (
                                <button
                                  className={`px-3 py-1 font-label-md border text-label-md transition-all rounded ${
                                    state === 'confirm'
                                      ? 'bg-error-container text-on-error-container border-error'
                                      : 'bg-primary-container text-primary border-outline-variant hover:bg-primary hover:text-on-primary'
                                  }`}
                                  onClick={() => handleReplay(row)}
                                >
                                  {state === 'confirm' ? 'Confirm?' : 'Replay'}
                                </button>
                              )}
                            </td>
                          </tr>
                        )
                      })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between p-md bg-surface-container-high border-t border-outline-variant">
            <p className="text-label-sm text-on-surface-variant">
              {data
                ? `Page ${page + 1} of ${data.totalPages} — ${data.totalElements} total events`
                : 'Loading...'}
            </p>
            <div className="flex items-center gap-xs">
              <button
                className="p-1 hover:bg-surface-variant rounded transition-colors text-on-surface-variant disabled:opacity-30"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <span className="text-label-sm text-on-surface px-2">{page + 1}</span>
              <button
                className="p-1 hover:bg-surface-variant rounded transition-colors text-on-surface-variant disabled:opacity-30"
                disabled={!data || page >= data.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                <span className="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        </div>

        {/* Terminal Log */}
        <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-md font-mono text-label-sm overflow-hidden relative">
          <div className="flex items-center justify-between mb-sm border-b border-outline-variant pb-xs">
            <span className="text-on-surface-variant flex items-center gap-xs">
              <span className="w-2 h-2 bg-secondary rounded-full animate-pulse" />
              Streaming Logs
            </span>
            <span className="text-[10px] text-outline">notify-dlq</span>
          </div>
          <div className="space-y-1 opacity-80">
            {[
              { time: new Date().toLocaleTimeString('en-US', { hour12: false }), level: 'INFO', color: 'text-secondary', msg: "Listening for new DLQ arrivals on queue 'notify-dlq'" },
              { time: new Date().toLocaleTimeString('en-US', { hour12: false }), level: 'INFO', color: 'text-secondary', msg: `Queue depth: ${data?.totalElements ?? 0} unreplayed events` },
            ].map((log, i) => (
              <div key={i} className="flex gap-md">
                <span className="text-outline">[{log.time}]</span>
                <span className={log.color}>{log.level}:</span>
                <span>{log.msg}</span>
              </div>
            ))}
          </div>
          <div className="absolute bottom-0 right-0 w-32 h-32 opacity-10 pointer-events-none">
            <span className="material-symbols-outlined text-[120px] text-secondary">terminal</span>
          </div>
        </div>
      </div>

      {/* FAB */}
      <button
        className="fixed bottom-lg right-lg w-14 h-14 bg-secondary text-on-secondary rounded-full flex items-center justify-center shadow-xl hover:scale-110 active:scale-95 transition-all z-50 group"
        onClick={refresh}
        title="Refresh DLQ"
      >
        <span className="material-symbols-outlined group-hover:rotate-180 transition-transform duration-500">replay</span>
      </button>
    </div>
  )
}
