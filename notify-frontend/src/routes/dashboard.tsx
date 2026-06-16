import { createFileRoute } from '@tanstack/react-router'
import { useState, useEffect, useRef } from 'react'
import { useDashboard, useFilterOptions, type DashboardFilters, EMPTY_FILTERS } from '@/hooks/useDashboard'

export const Route = createFileRoute('/dashboard')({
  component: Dashboard,
})

// ─── helpers ─────────────────────────────────────────────────────────────────

function lagStatus(lag: number): 'critical' | 'warning' | 'healthy' {
  if (lag > 100) return 'critical'
  if (lag > 10) return 'warning'
  return 'healthy'
}

const statusConfig = {
  critical: { label: 'Critical', textColor: 'text-red-400',   bg: 'bg-red-500/10',   border: 'border-red-500/20' },
  warning:  { label: 'Warning',  textColor: 'text-amber-400', bg: 'bg-amber-500/10', border: 'border-amber-500/20' },
  healthy:  { label: 'Healthy',  textColor: 'text-green-400', bg: 'bg-green-500/10', border: 'border-green-500/20' },
}

function fmt(d: Date) {
  return (
    d.toLocaleDateString('en-US', { month: 'short', day: '2-digit' }) +
    ', ' +
    d.toLocaleTimeString('en-US', { hour12: false })
  )
}

function getDefaultFilters(): DashboardFilters {
  const now   = new Date()
  const start = new Date(now)
  start.setMonth(start.getMonth() - 1)
  return {
    ...EMPTY_FILTERS,
    startDate: `${start.toISOString().slice(0, 10)}T00:00:00Z`,
    endDate:   `${now.toISOString().slice(0, 10)}T23:59:59Z`,
  }
}

// ─── CalendarPicker ──────────────────────────────────────────────────────────

const DOW = ['S', 'M', 'T', 'W', 'T', 'F', 'S']

function CalendarPicker({
  value,
  onSelect,
  placeholder,
  suffix,
}: {
  value: string       // full ISO e.g. "2026-06-16T00:00:00Z", or ""
  onSelect: (iso: string) => void
  placeholder: string
  suffix: string      // "T00:00:00Z" | "T23:59:59Z"
}) {
  const todayStr    = new Date().toISOString().slice(0, 10)
  const selectedStr = value ? value.slice(0, 10) : ''

  const seed = value ? new Date(value) : new Date()
  const [viewYear,  setViewYear]  = useState(seed.getFullYear())
  const [viewMonth, setViewMonth] = useState(seed.getMonth())
  const [open, setOpen] = useState(false)

  // Keep calendar view in sync when value is cleared/changed externally
  useEffect(() => {
    const d = value ? new Date(value) : new Date()
    setViewYear(d.getFullYear())
    setViewMonth(d.getMonth())
  }, [value])

  const display = value
    ? new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    : ''

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const startDow    = new Date(viewYear, viewMonth, 1).getDay()
  const cells       = [
    ...Array<null>(startDow).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ]
  const monthLabel = new Date(viewYear, viewMonth)
    .toLocaleString('en-US', { month: 'long', year: 'numeric' })

  function prevMonth() {
    if (viewMonth === 0) { setViewYear((y) => y - 1); setViewMonth(11) }
    else setViewMonth((m) => m - 1)
  }
  function nextMonth() {
    if (viewMonth === 11) { setViewYear((y) => y + 1); setViewMonth(0) }
    else setViewMonth((m) => m + 1)
  }
  function pickDay(day: number) {
    const d = [
      viewYear,
      String(viewMonth + 1).padStart(2, '0'),
      String(day).padStart(2, '0'),
    ].join('-')
    onSelect(`${d}${suffix}`)
    setOpen(false)
  }

  return (
    <div
      className="relative"
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null))
          setOpen(false)
      }}
    >
      {/* ── Trigger ── */}
      {/* div instead of button so the clear <button> inside is valid HTML */}
      <div
        role="button"
        tabIndex={0}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={(e) => e.key === 'Enter' && setOpen((o) => !o)}
        className="w-full flex items-center gap-sm bg-surface-container-lowest border border-outline-variant hover:border-primary/50 focus:border-primary rounded-lg px-sm py-[7px] transition-colors cursor-pointer outline-none"
      >
        <span className="material-symbols-outlined text-primary text-[18px] flex-shrink-0">
          calendar_month
        </span>
        <span className={`flex-1 text-body-sm select-none ${display ? 'text-on-surface' : 'text-outline'}`}>
          {display || placeholder}
        </span>
        {value ? (
          <button
            type="button"
            className="material-symbols-outlined text-[16px] text-on-surface-variant hover:text-error flex-shrink-0"
            onClick={(e) => { e.stopPropagation(); onSelect(''); setOpen(false) }}
          >
            close
          </button>
        ) : (
          <span
            className="material-symbols-outlined text-[16px] text-on-surface-variant/40 flex-shrink-0 transition-transform"
            style={{ transform: open ? 'rotate(180deg)' : '' }}
          >
            expand_more
          </span>
        )}
      </div>

      {/* ── Calendar popover ── */}
      {open && (
        <div className="absolute z-30 top-full left-0 mt-1 w-72 bg-surface-container-high border border-outline-variant rounded-xl shadow-2xl p-md select-none">

          {/* month navigation */}
          <div className="flex items-center justify-between mb-sm">
            <button
              type="button"
              onClick={prevMonth}
              className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-surface-variant text-on-surface-variant transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">chevron_left</span>
            </button>
            <span className="text-label-md font-bold text-on-surface">{monthLabel}</span>
            <button
              type="button"
              onClick={nextMonth}
              className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-surface-variant text-on-surface-variant transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">chevron_right</span>
            </button>
          </div>

          {/* day-of-week header */}
          <div className="grid grid-cols-7 mb-1">
            {DOW.map((d, i) => (
              <div key={i} className="h-8 flex items-center justify-center text-[10px] font-bold text-on-surface-variant uppercase">
                {d}
              </div>
            ))}
          </div>

          {/* day grid */}
          <div className="grid grid-cols-7">
            {cells.map((day, i) => {
              if (day === null) return <div key={i} className="h-9" />
              const dStr       = `${viewYear}-${String(viewMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
              const isSelected = dStr === selectedStr
              const isToday    = dStr === todayStr
              return (
                <button
                  key={i}
                  type="button"
                  onClick={() => pickDay(day)}
                  className={`h-9 w-9 mx-auto flex items-center justify-center rounded-full text-body-sm transition-colors ${
                    isSelected
                      ? 'bg-primary text-on-primary font-bold'
                      : isToday
                        ? 'ring-1 ring-primary text-primary font-bold hover:bg-primary/10'
                        : 'text-on-surface hover:bg-surface-variant'
                  }`}
                >
                  {day}
                </button>
              )
            })}
          </div>

          {/* Today shortcut */}
          <div className="mt-sm pt-sm border-t border-outline-variant/40 flex justify-center">
            <button
              type="button"
              className="text-label-sm text-primary hover:underline"
              onClick={() => { onSelect(`${todayStr}${suffix}`); setOpen(false) }}
            >
              Today
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── SkeletonRow ─────────────────────────────────────────────────────────────

function SkeletonRow() {
  return (
    <tr>
      {Array.from({ length: 5 }).map((_, i) => (
        <td key={i} className="px-md py-sm">
          <div className="h-3 bg-surface-variant rounded animate-pulse" />
        </td>
      ))}
    </tr>
  )
}

// ─── constants ───────────────────────────────────────────────────────────────

const dropdownBase =
  'w-full flex items-center justify-between bg-surface-container-lowest border border-outline-variant text-on-surface px-sm py-[7px] rounded-lg outline-none hover:border-primary/50 focus:border-primary transition-colors text-body-sm text-left'

// ─── Dashboard ───────────────────────────────────────────────────────────────

function Dashboard() {
  const [draft,   setDraft]   = useState<DashboardFilters>(getDefaultFilters)
  const [applied, setApplied] = useState<DashboardFilters>(getDefaultFilters)

  const [campaignDropOpen, setCampaignDropOpen] = useState(false)
  const [fileDropOpen,     setFileDropOpen]     = useState(false)

  const { metrics, lag, loading, error, lastRefresh, refresh } = useDashboard(applied)
  const { campaigns: campaignOptions, fileNames: fileOptions, loading: optionsLoading } = useFilterOptions()

  // Auto-select all campaigns once options have loaded
  const didAutoSelect = useRef(false)
  useEffect(() => {
    if (!didAutoSelect.current && campaignOptions.length > 0) {
      didAutoSelect.current = true
      const allIds = campaignOptions.map((c) => c.id)
      setDraft((d)   => ({ ...d, campaignIds: allIds }))
      setApplied((d) => ({ ...d, campaignIds: allIds }))
    }
  }, [campaignOptions])

  // ── derived ──────────────────────────────────────────────────────────────

  const activeFilterCount =
    [applied.startDate, applied.endDate, applied.uploadFileName].filter(Boolean).length +
    applied.campaignIds.length

  // ── handlers ─────────────────────────────────────────────────────────────

  function applyFilters() { setApplied({ ...draft }) }

  function clearFilters() {
    const d = getDefaultFilters()
    setDraft(d)
    setApplied(d)
  }

  function toggleCampaignId(id: number) {
    setDraft((d) => ({
      ...d,
      campaignIds: d.campaignIds.includes(id)
        ? d.campaignIds.filter((x) => x !== id)
        : [...d.campaignIds, id],
    }))
  }

  function selectFileName(name: string) {
    setDraft((d) => ({ ...d, uploadFileName: d.uploadFileName === name ? '' : name }))
    setFileDropOpen(false)
  }

  // ── cards ─────────────────────────────────────────────────────────────────

  const cards = metrics
    ? [
        { label: 'Total Sent',   value: metrics.totalSent,      icon: 'check_circle',    color: 'text-green-400',          trend: 'All channels' },
        { label: 'Failed / DLQ', value: metrics.totalFailed,    icon: 'release_alert',   color: 'text-red-400',            trend: 'Retries exhausted' },
        { label: 'Pending',      value: metrics.totalPending,   icon: 'hourglass_empty', color: 'text-amber-400',          trend: 'Active queues' },
        { label: 'Duplicates',   value: metrics.totalDuplicates,icon: 'content_copy',    color: 'text-blue-400',           trend: 'Deduplicated' },
        { label: 'Campaigns',    value: metrics.totalCampaigns, icon: 'flag',            color: 'text-on-surface-variant', trend: 'Total created' },
      ]
    : []

  // ── render ────────────────────────────────────────────────────────────────

  return (
    <div className="p-md max-w-[1600px] mx-auto">

      {/* ── Header ── */}
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-lg gap-md">
        <div>
          <h2 className="text-headline-md font-bold text-primary">System Performance Overview</h2>
          <p className="text-body-sm text-on-surface-variant">Real-time infrastructure and message delivery metrics.</p>
        </div>
        <div className="flex items-center gap-sm bg-surface-container rounded-lg px-sm py-xs border border-outline-variant">
          <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
          <span className="text-label-sm font-mono text-on-surface-variant">Last refreshed: {fmt(lastRefresh)}</span>
          <button
            onClick={refresh}
            className="ml-sm bg-primary-container text-on-primary-container px-sm py-xs rounded-md hover:bg-surface-variant transition-colors flex items-center gap-xs text-label-md"
          >
            <span className="material-symbols-outlined text-[18px]">refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* ── Filter Bar ── */}
      <div className="glass-panel rounded-xl p-lg mb-lg">

        {/* header row */}
        <div className="flex items-center justify-between mb-md">
          <div className="flex items-center gap-sm">
            <span className="material-symbols-outlined text-primary text-[20px]">filter_list</span>
            <h3 className="text-label-md font-bold text-on-surface uppercase tracking-widest">Filters</h3>
            {activeFilterCount > 0 && (
              <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-bold border border-primary/20 uppercase">
                {activeFilterCount} active
              </span>
            )}
          </div>
          <button
            onClick={clearFilters}
            className="flex items-center gap-xs text-label-sm text-on-surface-variant hover:text-error transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">restart_alt</span>
            Reset
          </button>
        </div>

        {/* filter grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-md">

          {/* Start Date */}
          <div className="space-y-xs">
            <label className="text-label-sm text-on-surface-variant uppercase tracking-wider block">
              Start Date
            </label>
            <CalendarPicker
              value={draft.startDate}
              onSelect={(iso) => setDraft((d) => ({ ...d, startDate: iso }))}
              placeholder="Start date"
              suffix="T00:00:00Z"
            />
          </div>

          {/* End Date */}
          <div className="space-y-xs">
            <label className="text-label-sm text-on-surface-variant uppercase tracking-wider block">
              End Date
            </label>
            <CalendarPicker
              value={draft.endDate}
              onSelect={(iso) => setDraft((d) => ({ ...d, endDate: iso }))}
              placeholder="End date"
              suffix="T23:59:59Z"
            />
          </div>

          {/* Campaign IDs — multi-select */}
          <div className="space-y-xs">
            <label className="text-label-sm text-on-surface-variant uppercase tracking-wider block">
              Campaigns
            </label>
            <div
              className="relative"
              onBlur={(e) => {
                if (!e.currentTarget.contains(e.relatedTarget as Node | null))
                  setCampaignDropOpen(false)
              }}
            >
              <button type="button" className={dropdownBase} onClick={() => setCampaignDropOpen((o) => !o)}>
                <span className={draft.campaignIds.length === 0 ? 'text-outline' : ''}>
                  {optionsLoading
                    ? 'Loading…'
                    : draft.campaignIds.length === 0
                      ? 'All campaigns'
                      : draft.campaignIds.length === campaignOptions.length
                        ? `All (${campaignOptions.length})`
                        : `${draft.campaignIds.length} of ${campaignOptions.length}`}
                </span>
                <span
                  className="material-symbols-outlined text-[18px] text-on-surface-variant flex-shrink-0 transition-transform"
                  style={{ transform: campaignDropOpen ? 'rotate(180deg)' : '' }}
                >
                  expand_more
                </span>
              </button>

              {campaignDropOpen && (
                <div className="absolute z-20 top-full left-0 right-0 mt-1 bg-surface-container-high border border-outline-variant rounded-lg shadow-2xl max-h-52 overflow-y-auto">
                  {campaignOptions.length > 0 && (
                    <button
                      className="w-full text-left px-md py-sm hover:bg-surface-variant text-label-sm font-bold text-primary border-b border-outline-variant"
                      onClick={() => {
                        const allIds   = campaignOptions.map((c) => c.id)
                        const allChked = allIds.every((id) => draft.campaignIds.includes(id))
                        setDraft((d) => ({ ...d, campaignIds: allChked ? [] : allIds }))
                      }}
                    >
                      {campaignOptions.every((c) => draft.campaignIds.includes(c.id))
                        ? 'Deselect all'
                        : 'Select all'}
                    </button>
                  )}

                  {optionsLoading ? (
                    <div className="px-md py-sm text-body-sm text-on-surface-variant">Loading…</div>
                  ) : campaignOptions.length === 0 ? (
                    <div className="px-md py-sm text-body-sm text-on-surface-variant">No campaigns found</div>
                  ) : (
                    campaignOptions.map((c) => (
                      <label
                        key={c.id}
                        className="flex items-center gap-sm px-md py-sm hover:bg-surface-variant cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={draft.campaignIds.includes(c.id)}
                          onChange={() => toggleCampaignId(c.id)}
                          className="accent-primary flex-shrink-0"
                        />
                        <div className="min-w-0">
                          <span className="text-body-sm text-on-surface block truncate">{c.campaignName}</span>
                          <span className="text-[10px] text-on-surface-variant font-mono">ID: {c.id}</span>
                        </div>
                      </label>
                    ))
                  )}
                </div>
              )}
            </div>

            {/* selected tags (only shown for partial selections) */}
            {draft.campaignIds.length > 0 && draft.campaignIds.length < campaignOptions.length && (
              <div className="flex flex-wrap gap-xs pt-xs">
                {draft.campaignIds.map((id) => {
                  const name = campaignOptions.find((c) => c.id === id)?.campaignName ?? `ID ${id}`
                  return (
                    <span
                      key={id}
                      className="inline-flex items-center gap-xs px-sm py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-bold border border-primary/20"
                    >
                      {name}
                      <button
                        onClick={() => toggleCampaignId(id)}
                        className="material-symbols-outlined text-[12px] hover:text-error leading-none"
                      >
                        close
                      </button>
                    </span>
                  )
                })}
              </div>
            )}
          </div>

          {/* Upload File Name — single-select */}
          <div className="space-y-xs">
            <label className="text-label-sm text-on-surface-variant uppercase tracking-wider block">
              Upload File Name
            </label>
            <div
              className="relative"
              onBlur={(e) => {
                if (!e.currentTarget.contains(e.relatedTarget as Node | null))
                  setFileDropOpen(false)
              }}
            >
              <button type="button" className={dropdownBase} onClick={() => setFileDropOpen((o) => !o)}>
                <span className={!draft.uploadFileName ? 'text-outline' : 'font-mono'}>
                  {draft.uploadFileName || 'All files'}
                </span>
                <span
                  className="material-symbols-outlined text-[18px] text-on-surface-variant flex-shrink-0 transition-transform"
                  style={{ transform: fileDropOpen ? 'rotate(180deg)' : '' }}
                >
                  expand_more
                </span>
              </button>

              {fileDropOpen && (
                <div className="absolute z-20 top-full left-0 right-0 mt-1 bg-surface-container-high border border-outline-variant rounded-lg shadow-2xl max-h-52 overflow-y-auto">
                  <button
                    className={`w-full text-left px-md py-sm hover:bg-surface-variant text-body-sm border-b border-outline-variant ${
                      !draft.uploadFileName ? 'text-primary font-bold bg-primary/5' : 'text-on-surface-variant'
                    }`}
                    onClick={() => { setDraft((d) => ({ ...d, uploadFileName: '' })); setFileDropOpen(false) }}
                  >
                    All files
                  </button>

                  {optionsLoading ? (
                    <div className="px-md py-sm text-body-sm text-on-surface-variant">Loading…</div>
                  ) : fileOptions.length === 0 ? (
                    <div className="px-md py-sm text-body-sm text-on-surface-variant">No files found</div>
                  ) : (
                    fileOptions.map((name) => (
                      <button
                        key={name}
                        className={`w-full text-left px-md py-sm hover:bg-surface-variant font-mono text-body-sm ${
                          draft.uploadFileName === name ? 'text-primary font-bold bg-primary/5' : 'text-on-surface'
                        }`}
                        onClick={() => selectFileName(name)}
                      >
                        {name}
                      </button>
                    ))
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* footer row */}
        <div className="flex items-center justify-between mt-md pt-md border-t border-outline-variant/50">
          {activeFilterCount > 0 ? (
            <span className="flex items-center gap-xs text-label-sm text-amber-400">
              <span className="material-symbols-outlined text-[16px]">bolt</span>
              Filters active — results are live (cache bypassed)
            </span>
          ) : (
            <span className="text-label-sm text-on-surface-variant">
              No filters applied — results cached for 30s
            </span>
          )}
          <button
            onClick={applyFilters}
            className="bg-primary-container text-on-primary-container px-lg py-sm rounded-lg text-label-md font-bold hover:brightness-110 transition-all border border-outline-variant"
          >
            Apply Filters
          </button>
        </div>
      </div>

      {/* ── Error ── */}
      {error && (
        <div className="mb-lg p-md bg-error-container/20 border border-error/30 rounded-xl flex items-center gap-md">
          <span className="material-symbols-outlined text-error flex-shrink-0">warning</span>
          <span className="text-body-sm text-on-surface">{error}</span>
        </div>
      )}

      {/* ── Metric Cards ── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-md mb-lg">
        {loading
          ? Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="tonal-layer-1 p-md rounded-xl space-y-sm animate-pulse">
                <div className="h-3 w-1/2 bg-surface-variant rounded" />
                <div className="h-8 w-2/3 bg-surface-variant rounded" />
                <div className="h-3 w-3/4 bg-surface-variant rounded" />
              </div>
            ))
          : cards.map((card) => (
              <div key={card.label} className="tonal-layer-1 p-md rounded-xl flex flex-col gap-xs">
                <div className="flex items-center justify-between">
                  <span className="text-label-sm text-on-surface-variant uppercase tracking-wider">{card.label}</span>
                  <span className={`material-symbols-outlined text-[20px] ${card.color}`}>{card.icon}</span>
                </div>
                <div className={`text-display font-bold ${card.color}`}>
                  {card.value.toLocaleString()}
                </div>
                <div className="text-label-sm text-on-surface-variant">{card.trend}</div>
              </div>
            ))}
      </div>

      {/* ── Consumer Lag Table ── */}
      <div className="tonal-layer-1 rounded-xl overflow-hidden mb-lg">
        <div className="bg-surface-container p-md border-b border-outline-variant flex items-center justify-between">
          <div className="flex items-center gap-sm">
            <span className="material-symbols-outlined text-primary">hub</span>
            <h3 className="text-headline-md font-bold">Kafka Consumer Group Lag</h3>
          </div>
          <div className="flex items-center gap-md">
            {(['critical', 'warning', 'healthy'] as const).map((s) => (
              <div key={s} className="flex items-center gap-xs">
                <div className={`w-2 h-2 rounded-full ${statusConfig[s].bg.replace('/10', '')}`} />
                <span className="text-label-sm text-on-surface-variant capitalize">
                  {s === 'critical' ? 'Critical (>100)' : s === 'warning' ? 'Warning (>10)' : 'Healthy'}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse font-mono text-body-sm">
            <thead>
              <tr className="bg-surface-container-high border-b border-outline-variant text-label-sm uppercase tracking-wider text-on-surface-variant">
                <th className="px-md py-sm">Group ID</th>
                <th className="px-md py-sm">Topic</th>
                <th className="px-md py-sm">Partition</th>
                <th className="px-md py-sm text-right">Lag</th>
                <th className="px-md py-sm text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading
                ? Array.from({ length: 3 }).map((_, i) => <SkeletonRow key={i} />)
                : lag.length === 0
                  ? (
                    <tr>
                      <td colSpan={5} className="px-md py-xl text-center text-on-surface-variant text-body-sm">
                        No consumer lag data available.
                      </td>
                    </tr>
                  )
                  : lag.map((row, idx) => {
                    const s   = lagStatus(row.lag)
                    const cfg = statusConfig[s]
                    return (
                      <tr key={idx} className="hover:bg-surface-variant transition-colors">
                        <td className="px-md py-sm text-on-surface font-medium">{row.groupId}</td>
                        <td className="px-md py-sm text-on-surface-variant">{row.topic}</td>
                        <td className="px-md py-sm text-on-surface-variant">{row.partition}</td>
                        <td className={`px-md py-sm text-right font-bold ${cfg.textColor}`}>
                          {row.lag.toLocaleString()}
                        </td>
                        <td className="px-md py-sm text-center">
                          <span className={`inline-block px-2 py-0.5 rounded-full ${cfg.bg} ${cfg.textColor} text-label-sm uppercase font-bold border ${cfg.border}`}>
                            {cfg.label}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Infrastructure Grid ── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-md">
        <div className="tonal-layer-1 p-md rounded-xl flex flex-col gap-md">
          <div className="flex items-center justify-between border-b border-outline-variant pb-sm">
            <h4 className="text-label-md font-bold uppercase tracking-widest text-on-surface-variant">Node Health (Kubernetes)</h4>
            <span className="material-symbols-outlined text-green-400">dns</span>
          </div>
          <div className="grid grid-cols-4 gap-sm">
            {[
              { name: 'worker-a-1', color: 'bg-green-500' },
              { name: 'worker-a-2', color: 'bg-green-500' },
              { name: 'worker-b-1', color: 'bg-amber-500', pulse: true },
              { name: 'worker-b-2', color: 'bg-green-500' },
            ].map((node) => (
              <div key={node.name} className="flex flex-col gap-1">
                <div className={`h-1 ${node.color} rounded-full ${node.pulse ? 'animate-pulse' : ''}`} />
                <span className="text-[9px] font-mono uppercase text-on-surface-variant">{node.name}</span>
              </div>
            ))}
          </div>
          <p className="text-label-sm text-on-surface-variant italic">Node worker-b-1 at 88% CPU utilization.</p>
        </div>

        <div className="tonal-layer-1 p-md rounded-xl flex flex-col gap-md">
          <div className="flex items-center justify-between border-b border-outline-variant pb-sm">
            <h4 className="text-label-md font-bold uppercase tracking-widest text-on-surface-variant">Active Incidents</h4>
            <span className="material-symbols-outlined text-red-400">campaign</span>
          </div>
          {!loading && metrics && metrics.totalFailed > 0 ? (
            <div className="flex items-center gap-md">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-red-500/10 flex items-center justify-center border border-red-500/20">
                <span className="material-symbols-outlined text-red-400">priority_high</span>
              </div>
              <div>
                <p className="text-label-md font-bold text-on-surface">
                  {metrics.totalFailed} event{metrics.totalFailed !== 1 ? 's' : ''} in Dead Letter Queue
                </p>
                <p className="text-body-sm text-on-surface-variant">Manual intervention required.</p>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-md">
              <div className="w-10 h-10 rounded-lg bg-green-500/10 flex items-center justify-center border border-green-500/20">
                <span className="material-symbols-outlined text-green-400">check_circle</span>
              </div>
              <span className="text-body-sm text-on-surface-variant">
                {loading ? 'Loading...' : 'No active incidents.'}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
