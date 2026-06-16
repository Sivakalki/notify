import { createFileRoute } from '@tanstack/react-router'
import { useState } from 'react'
import { useCohorts } from '@/hooks/useCohorts'
import type { Cohort, AddUsersResult } from '@/hooks/useCohorts'

export const Route = createFileRoute('/cohorts')({
  component: Cohorts,
})

function Cohorts() {
  const { data: cohorts, loading, error, createCohort, addUsers, refresh } = useCohorts()

  const [filter, setFilter] = useState('')
  const [selectedCohort, setSelectedCohort] = useState<Cohort | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalName, setModalName] = useState('')
  const [modalDesc, setModalDesc] = useState('')
  const [modalSubmitting, setModalSubmitting] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)

  const [addInput, setAddInput] = useState('')
  const [addLoading, setAddLoading] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)
  const [addResult, setAddResult] = useState<AddUsersResult | null>(null)

  const filtered = filter
    ? cohorts.filter((c) => c.name.toLowerCase().includes(filter.toLowerCase()))
    : cohorts

  function openModal() {
    setModalOpen(true)
    setModalName('')
    setModalDesc('')
    setModalError(null)
  }

  async function handleCreateCohort() {
    if (!modalName.trim()) return
    setModalSubmitting(true)
    setModalError(null)
    try {
      await createCohort({ name: modalName.trim(), description: modalDesc.trim() })
      setModalOpen(false)
    } catch (err) {
      setModalError(err instanceof Error ? err.message : 'Failed to create cohort')
    } finally {
      setModalSubmitting(false)
    }
  }

  async function handleAddUsers() {
    if (!selectedCohort) return
    const lines = addInput.split('\n').map((l) => l.trim()).filter(Boolean)
    if (!lines.length) return
    setAddLoading(true)
    setAddError(null)
    try {
      const result = await addUsers(selectedCohort.id, lines.map((externalUserId) => ({ externalUserId })))
      setAddResult(result)
      setAddInput('')
      refresh()
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Failed to add users')
    } finally {
      setAddLoading(false)
    }
  }

  function selectCohort(c: Cohort) {
    if (selectedCohort?.id === c.id) {
      setSelectedCohort(null)
    } else {
      setSelectedCohort(c)
      setAddInput('')
      setAddResult(null)
      setAddError(null)
    }
  }

  const totalMembers = cohorts.reduce((s, c) => s + c.memberCount, 0)

  return (
    <div className="min-h-screen p-lg overflow-x-hidden">
      <div className="max-w-[1400px] mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-lg">
          <div>
            <h2 className="text-headline-md font-headline-md text-on-surface">Cohorts</h2>
            <p className="text-body-sm text-on-surface-variant">Manage and segment your user groups for targeted campaigns.</p>
          </div>
          <button
            className="flex items-center gap-xs bg-primary-container border border-outline-variant px-md py-sm rounded-lg hover:bg-surface-variant transition-colors text-on-primary-container text-label-md"
            onClick={openModal}
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            New Cohort
          </button>
        </div>

        {error && (
          <div className="mb-lg p-md bg-error-container/20 border border-error/30 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-error">warning</span>
            <span className="text-body-sm text-on-surface">{error}</span>
          </div>
        )}

        {/* Bento Layout */}
        <div className="grid grid-cols-12 gap-md mb-lg">
          {/* Table */}
          <div className="col-span-12 lg:col-span-8 bg-surface-container-low border border-outline-variant rounded-xl overflow-hidden">
            <div className="px-md py-sm border-b border-outline-variant flex items-center justify-between">
              <span className="text-label-sm text-on-surface-variant uppercase tracking-wider">
                Active Cohorts{!loading && ` (${cohorts.length})`}
              </span>
              <div className="flex items-center gap-xs border border-outline-variant rounded-lg px-2 py-1 bg-surface-container-lowest">
                <span className="material-symbols-outlined text-[16px] text-outline">search</span>
                <input
                  className="bg-transparent border-none outline-none text-label-sm w-48 text-on-surface"
                  placeholder="Filter cohorts..."
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                />
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-surface-container-high border-b border-outline-variant">
                    {['Name', 'Description', 'Member Count', 'Created Date', ''].map((h) => (
                      <th key={h} className="px-md py-3 text-label-sm text-on-surface-variant uppercase tracking-tighter">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant">
                  {loading
                    ? Array.from({ length: 4 }).map((_, i) => (
                      <tr key={i}>
                        {Array.from({ length: 5 }).map((__, j) => (
                          <td key={j} className="px-md py-4">
                            <div className="h-4 bg-surface-variant rounded animate-pulse" />
                          </td>
                        ))}
                      </tr>
                    ))
                    : filtered.length === 0
                      ? (
                        <tr>
                          <td colSpan={5} className="px-md py-xl text-center text-on-surface-variant text-body-sm">
                            No cohorts found.
                          </td>
                        </tr>
                      )
                      : filtered.map((c) => (
                        <tr
                          key={c.id}
                          className={`hover:bg-surface-container-high transition-colors cursor-pointer ${selectedCohort?.id === c.id ? 'bg-surface-variant/20' : ''}`}
                          onClick={() => selectCohort(c)}
                        >
                          <td className="px-md py-4 text-body-md text-on-surface font-medium">{c.name}</td>
                          <td className="px-md py-4 text-body-sm text-on-surface-variant">{c.description || '—'}</td>
                          <td className="px-md py-4 text-body-sm text-on-surface font-mono">{c.memberCount.toLocaleString()}</td>
                          <td className="px-md py-4 text-body-sm text-on-surface-variant">
                            {new Date(c.createdAt).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })}
                          </td>
                          <td className="px-md py-4 text-right">
                            <span className="material-symbols-outlined text-outline hover:text-on-surface">chevron_right</span>
                          </td>
                        </tr>
                      ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Stats Panel */}
          <div className="col-span-12 lg:col-span-4 flex flex-col gap-md">
            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-md">
              <h3 className="text-label-sm text-on-surface-variant uppercase tracking-wider mb-sm">Quick Insights</h3>
              <div className="space-y-md">
                <div className="flex items-center justify-between">
                  <span className="text-body-sm text-on-surface-variant">Total Cohorts</span>
                  <span className="text-headline-md font-headline-md text-on-surface font-mono">
                    {loading ? '—' : cohorts.length}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-body-sm text-on-surface-variant">Total Members</span>
                  <span className="text-headline-md font-headline-md text-on-surface font-mono">
                    {loading ? '—' : totalMembers.toLocaleString()}
                  </span>
                </div>
              </div>
            </div>

            <div className="bg-primary-container/10 border border-primary/20 rounded-xl p-md relative overflow-hidden group">
              <div className="absolute -right-4 -bottom-4 opacity-10 group-hover:opacity-20 transition-opacity">
                <span className="material-symbols-outlined text-[80px]">auto_awesome</span>
              </div>
              <h3 className="text-label-sm text-primary uppercase tracking-wider mb-xs">Smart Cohorts</h3>
              <p className="text-body-sm text-on-primary-container mb-md leading-relaxed">
                Let AI automatically cluster your users based on notification interaction patterns.
              </p>
              <button className="w-full py-2 bg-primary text-on-primary text-label-md rounded-lg hover:opacity-90 transition-opacity">
                Enable Smart Sync
              </button>
            </div>
          </div>
        </div>

        {/* Detail View */}
        {selectedCohort && (
          <div>
            <div className="flex items-center gap-sm mb-md">
              <button
                className="p-1 hover:bg-surface-variant rounded-full transition-colors"
                onClick={() => setSelectedCohort(null)}
              >
                <span className="material-symbols-outlined">arrow_back</span>
              </button>
              <h3 className="text-headline-md font-headline-md text-on-surface">Cohort: {selectedCohort.name}</h3>
              <span className="px-2 py-0.5 bg-secondary-container text-on-secondary-container text-label-sm rounded uppercase ml-2">
                {selectedCohort.memberCount.toLocaleString()} members
              </span>
            </div>

            <div className="grid grid-cols-12 gap-lg">
              {/* Info Card */}
              <div className="col-span-12 xl:col-span-8 bg-surface-container-low border border-outline-variant rounded-xl overflow-hidden">
                <div className="px-md py-sm border-b border-outline-variant">
                  <span className="text-label-sm text-on-surface-variant uppercase tracking-wider">Cohort Details</span>
                </div>
                <div className="p-lg">
                  <div className="grid grid-cols-2 gap-md">
                    <div className="bg-surface-container p-md rounded-lg border border-outline-variant">
                      <p className="text-label-sm text-on-surface-variant mb-1">ID</p>
                      <p className="font-mono text-on-surface">{selectedCohort.id}</p>
                    </div>
                    <div className="bg-surface-container p-md rounded-lg border border-outline-variant">
                      <p className="text-label-sm text-on-surface-variant mb-1">Members</p>
                      <p className="font-mono text-on-surface text-headline-md font-bold">
                        {selectedCohort.memberCount.toLocaleString()}
                      </p>
                    </div>
                    <div className="col-span-2 bg-surface-container p-md rounded-lg border border-outline-variant">
                      <p className="text-label-sm text-on-surface-variant mb-1">Description</p>
                      <p className="text-on-surface text-body-sm">{selectedCohort.description || '—'}</p>
                    </div>
                    <div className="col-span-2 bg-surface-container p-md rounded-lg border border-outline-variant">
                      <p className="text-label-sm text-on-surface-variant mb-1">Created At</p>
                      <p className="text-on-surface text-body-sm font-mono">
                        {new Date(selectedCohort.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Add Users Panel */}
              <div className="col-span-12 xl:col-span-4 flex flex-col gap-md">
                <div className="bg-surface-container-low border border-outline-variant rounded-xl p-md">
                  <h3 className="text-label-sm text-on-surface-variant uppercase tracking-wider mb-md">Add Users</h3>
                  <p className="text-body-sm text-on-surface-variant mb-sm">
                    Paste external user IDs — one per line.
                  </p>
                  <textarea
                    className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg p-sm text-body-sm font-mono text-on-surface focus:ring-1 focus:ring-secondary focus:border-secondary outline-none transition-all placeholder:text-outline"
                    placeholder={'user-001\nuser-002\nuser-003'}
                    rows={8}
                    value={addInput}
                    onChange={(e) => setAddInput(e.target.value)}
                  />
                  {addError && (
                    <p className="text-error text-body-sm mt-sm">{addError}</p>
                  )}
                  <button
                    className="w-full mt-lg py-2.5 bg-on-surface text-surface text-label-md rounded-lg hover:opacity-90 transition-all flex items-center justify-center gap-sm disabled:opacity-50"
                    onClick={handleAddUsers}
                    disabled={addLoading || !addInput.trim()}
                  >
                    {addLoading ? (
                      <>
                        <span className="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
                        Adding...
                      </>
                    ) : (
                      <>
                        <span className="material-symbols-outlined text-[18px]">group_add</span>
                        Add to Cohort
                      </>
                    )}
                  </button>
                </div>

                {addResult && (
                  <div className="bg-surface-container-high border border-outline-variant rounded-xl p-md">
                    <h4 className="text-label-sm text-on-surface uppercase tracking-wider mb-md">Processing Results</h4>
                    <div className="grid grid-cols-3 gap-sm">
                      {[
                        { label: 'Total', value: addResult.total, color: 'text-on-surface' },
                        { label: 'Added', value: addResult.added, color: 'text-secondary' },
                        { label: 'Dupes', value: addResult.duplicates, color: 'text-on-surface-variant' },
                      ].map((r) => (
                        <div key={r.label} className="bg-surface-container-lowest p-sm rounded border border-outline-variant text-center">
                          <p className="text-label-sm text-on-surface-variant">{r.label}</p>
                          <p className={`font-mono text-headline-md ${r.color}`}>{r.value}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* New Cohort Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-md">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setModalOpen(false)} />
          <div className="relative bg-surface-container-low border border-outline-variant rounded-xl  overflow-hidden shadow-2xl">
            <div className="p-md border-b border-outline-variant flex items-center justify-between">
              <h3 className="text-headline-md font-headline-md">Create New Cohort</h3>
              <button className="text-outline hover:text-on-surface" onClick={() => setModalOpen(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-md space-y-md">
              <div className="space-y-1">
                <label className="text-label-sm text-on-surface-variant uppercase">
                  Cohort Name <span className="text-error">*</span>
                </label>
                <input
                  className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-2 focus:ring-1 focus:ring-primary focus:border-primary outline-none"
                  placeholder="e.g., Inactive 30 Days"
                  value={modalName}
                  onChange={(e) => setModalName(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCreateCohort()}
                />
              </div>
              <div className="space-y-1">
                <label className="text-label-sm text-on-surface-variant uppercase">Description</label>
                <textarea
                  className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-2 focus:ring-1 focus:ring-primary focus:border-primary outline-none"
                  placeholder="Optional description..."
                  rows={3}
                  value={modalDesc}
                  onChange={(e) => setModalDesc(e.target.value)}
                />
              </div>
              {modalError && (
                <p className="text-error text-body-sm">{modalError}</p>
              )}
            </div>
            <div className="p-md bg-surface-container-high flex justify-end gap-sm">
              <button
                className="px-md py-2 text-label-md text-on-surface-variant hover:text-on-surface"
                onClick={() => setModalOpen(false)}
              >
                Cancel
              </button>
              <button
                className="px-md py-2 bg-primary text-on-primary rounded-lg text-label-md disabled:opacity-50"
                onClick={handleCreateCohort}
                disabled={modalSubmitting || !modalName.trim()}
              >
                {modalSubmitting ? 'Creating...' : 'Create Cohort'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
