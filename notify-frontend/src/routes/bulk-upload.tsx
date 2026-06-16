import { createFileRoute, Link } from '@tanstack/react-router'
import { useState, useRef } from 'react'
import { useCampaigns } from '@/hooks/useCampaigns'
import { apiFetch, hasApiKey } from '@/lib/api'

export const Route = createFileRoute('/bulk-upload')({
  component: BulkUpload,
})

interface UploadResult {
  uploadId: number
  campaignId: number
  fileName: string
  fileType: string
  rowCount: number
  duplicateCount: number
  status: string
  createdAt: string
}

function BulkUpload() {
  const { data: campaignsPage, loading: campaignsLoading } = useCampaigns(0, 100)
  const campaigns = campaignsPage?.content ?? []

  const [campaignId, setCampaignId] = useState<number | ''>('')
  const [dragging, setDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<UploadResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const noKey = !hasApiKey()

  function handleFileSelect(file: File) {
    setSelectedFile(file)
    setResult(null)
    setError(null)
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFileSelect(file)
  }

  async function handleUpload() {
    if (!campaignId || !selectedFile) return
    setUploading(true)
    setError(null)
    try {
      const form = new FormData()
      form.append('campaignId', String(campaignId))
      form.append('file', selectedFile)
      const res = await apiFetch<UploadResult>('/api/v1/notifications/bulk-upload', {
        method: 'POST',
        body: form,
      })
      setResult(res)
      setSelectedFile(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const columns = [
    { name: 'externalUserId', badge: 'Required', badgeClass: 'bg-error-container text-on-error-container', nameClass: 'text-primary', desc: 'Primary key for user mapping.' },
    { name: 'email', badge: 'Optional', badgeClass: 'bg-surface-variant text-on-surface-variant', nameClass: 'text-on-surface', desc: 'Valid email for SMTP delivery.' },
    { name: 'phone', badge: 'Optional', badgeClass: 'bg-surface-variant text-on-surface-variant', nameClass: 'text-on-surface', desc: 'E.164 format for SMS/WhatsApp.' },
  ]

  return (
    <div className="min-h-screen p-lg">
      <div className="max-w-[1200px] mx-auto space-y-lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-md border-b border-outline-variant pb-lg">
          <div>
            <h2 className="text-display font-bold text-on-surface">Bulk Upload</h2>
            <p className="text-body-md text-on-surface-variant mt-1">
              Import large volumes of user data to trigger transactional sequences.
            </p>
          </div>
          <a
            href="/sample.csv"
            download
            className="flex items-center gap-sm bg-primary-container text-on-primary-container px-lg py-sm rounded-lg border border-outline hover:bg-surface-variant transition-colors text-label-md"
          >
            <span className="material-symbols-outlined text-[18px]">download</span>
            Download sample CSV
          </a>
        </div>

        {noKey && (
          <div className="p-md bg-amber-500/10 border border-amber-500/20 rounded-xl flex items-center gap-md">
            <span className="material-symbols-outlined text-amber-400 flex-shrink-0">key_off</span>
            <p className="text-body-sm text-on-surface">
              No API key configured.{' '}
              <Link to="/api-keys" className="text-secondary underline">Register a client</Link> first.
            </p>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-lg items-start">
          {/* Left */}
          <div className="lg:col-span-8 space-y-lg">
            {/* Campaign selector */}
            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-md">
              <label className="text-label-md text-on-surface-variant uppercase tracking-wider block mb-sm">
                Select Campaign <span className="text-error">*</span>
              </label>
              <div className="relative">
                <select
                  className="w-full bg-surface-container-lowest border border-outline-variant rounded-lg p-md text-on-surface appearance-none focus:border-primary outline-none disabled:opacity-50"
                  value={campaignId}
                  onChange={(e) => setCampaignId(Number(e.target.value))}
                  disabled={campaignsLoading}
                >
                  <option value="" disabled>
                    {campaignsLoading ? 'Loading...' : campaigns.length === 0 ? 'No campaigns' : 'Choose a campaign to target'}
                  </option>
                  {campaigns.map((c) => (
                    <option key={c.id} value={c.id}>{c.campaignName} ({c.channel})</option>
                  ))}
                </select>
                <span className="material-symbols-outlined absolute right-md top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">
                  expand_more
                </span>
              </div>
            </div>

            {/* Dropzone */}
            <div
              className={`relative min-h-[280px] flex flex-col items-center justify-center border-2 border-dashed rounded-xl p-xl transition-all cursor-pointer group ${dragging ? 'border-primary bg-surface-container' : 'border-outline-variant bg-surface-container hover:bg-surface-container-high'}`}
              onDragEnter={(e) => { e.preventDefault(); setDragging(true) }}
              onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
              onDragLeave={() => setDragging(false)}
              onDrop={handleDrop}
              onClick={() => fileRef.current?.click()}
            >
              <div className="flex flex-col items-center text-center">
                <div className="w-16 h-16 bg-surface-container-highest rounded-full flex items-center justify-center mb-md border border-outline-variant group-hover:scale-110 transition-transform">
                  <span className="material-symbols-outlined text-[32px] text-primary">cloud_upload</span>
                </div>
                {selectedFile ? (
                  <>
                    <h3 className="text-headline-md font-bold text-on-surface">{selectedFile.name}</h3>
                    <p className="text-body-sm text-on-surface-variant mt-1">
                      {(selectedFile.size / 1024).toFixed(1)} KB · Click to change file
                    </p>
                  </>
                ) : (
                  <>
                    <h3 className="text-headline-md font-bold text-on-surface">Drop your file here</h3>
                    <p className="text-body-md text-on-surface-variant mt-2 mb-lg">
                      .csv, .xlsx, or .json · Max 50MB
                    </p>
                    <button
                      className="bg-primary text-on-primary px-xl py-md rounded-lg font-bold hover:opacity-90 active:scale-95 transition-all"
                      onClick={(e) => { e.stopPropagation(); fileRef.current?.click() }}
                    >
                      Select File
                    </button>
                  </>
                )}
                <input
                  ref={fileRef}
                  accept=".csv,.xlsx,.json"
                  className="hidden"
                  type="file"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileSelect(e.target.files[0]) }}
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
              <div className="absolute bottom-4 right-4 flex gap-xs">
                {['.CSV', '.XLSX', '.JSON'].map((ext) => (
                  <div key={ext} className="px-2 py-1 rounded bg-surface-container-lowest border border-outline-variant text-[10px] text-on-surface-variant font-mono">
                    {ext}
                  </div>
                ))}
              </div>
            </div>

            {/* Upload Button */}
            {selectedFile && (
              <button
                className="w-full py-md bg-primary text-on-primary rounded-xl font-bold text-label-md flex items-center justify-center gap-sm hover:opacity-90 disabled:opacity-50 transition-all"
                onClick={handleUpload}
                disabled={!campaignId || uploading || noKey}
              >
                {uploading ? (
                  <>
                    <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Uploading...
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined">upload</span>
                    Upload {selectedFile.name}
                  </>
                )}
              </button>
            )}

            {error && (
              <div className="p-md bg-error-container/20 border border-error/30 rounded-xl text-error text-body-sm flex items-center gap-sm">
                <span className="material-symbols-outlined">error</span>
                {error}
              </div>
            )}

            {/* Accepted Columns */}
            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-lg">
              <div className="flex items-center gap-sm mb-md">
                <span className="material-symbols-outlined text-primary">info</span>
                <h4 className="text-label-md font-bold uppercase tracking-wider text-on-surface">Accepted Columns</h4>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-md">
                {columns.map((col) => (
                  <div key={col.name} className="p-md bg-surface-container rounded-lg border border-outline-variant">
                    <div className="flex justify-between items-start mb-1">
                      <span className={`font-mono text-[12px] font-bold ${col.nameClass}`}>{col.name}</span>
                      <span className={`text-[9px] px-1.5 py-0.5 rounded uppercase font-bold ${col.badgeClass}`}>
                        {col.badge}
                      </span>
                    </div>
                    <p className="text-body-sm text-on-surface-variant">{col.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right */}
          <div className="lg:col-span-4 space-y-lg">
            {/* Result Card */}
            <div className="bg-surface-container-high border border-outline rounded-xl overflow-hidden shadow-xl">
              <div className="bg-primary-container p-md border-b border-outline">
                <div className="flex items-center gap-sm">
                  <span className="material-symbols-outlined text-primary">analytics</span>
                  <h4 className="text-label-md font-bold uppercase tracking-wider text-on-primary">Upload Result</h4>
                </div>
              </div>
              <div className="p-lg space-y-lg">
                {result ? (
                  <>
                    <div className="flex justify-between items-center border-b border-outline-variant pb-md">
                      <span className="text-on-surface-variant text-body-sm">Upload ID</span>
                      <span className="font-mono text-primary text-[13px] bg-surface-container-lowest px-2 py-1 rounded">
                        #{result.uploadId}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">File</span>
                      <span className="text-on-surface text-body-sm font-medium">{result.fileName}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">Type</span>
                      <span className="font-mono text-on-surface text-body-sm">{result.fileType}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">Campaign ID</span>
                      <span className="font-mono text-on-surface text-body-sm">{result.campaignId}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">Rows</span>
                      <span className="font-mono text-on-surface text-body-sm">{result.rowCount.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">Duplicates</span>
                      <span className="font-mono text-on-surface text-body-sm">{result.duplicateCount.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-on-surface-variant text-body-sm">Status</span>
                      <span className="px-sm py-xs bg-amber-500/10 text-amber-400 text-label-sm rounded-full border border-amber-500/20 uppercase font-bold">
                        {result.status}
                      </span>
                    </div>
                    <button
                      className="w-full py-md bg-surface-container-lowest border border-outline-variant hover:bg-surface-variant rounded-lg text-label-md text-on-surface flex items-center justify-center gap-sm transition-colors"
                      onClick={() => setResult(null)}
                    >
                      <span className="material-symbols-outlined text-[18px]">add</span>
                      New Upload
                    </button>
                  </>
                ) : (
                  <div className="text-center py-xl text-on-surface-variant text-body-sm">
                    <span className="material-symbols-outlined text-[40px] block mb-sm text-outline">upload_file</span>
                    No recent upload.
                  </div>
                )}
              </div>
            </div>

            {/* Pro Tip */}
            <div className="p-lg bg-tertiary-container rounded-xl border border-tertiary/20">
              <h5 className="text-on-tertiary-container font-bold text-label-md mb-2 flex items-center gap-2">
                <span className="material-symbols-outlined text-[16px]">lightbulb</span>
                PRO TIP
              </h5>
              <p className="text-body-sm text-on-tertiary-container/80 leading-relaxed">
                For files over 500k rows, use our <span className="font-mono text-tertiary">s3-direct</span> integration for better performance.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
