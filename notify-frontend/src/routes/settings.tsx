import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'
import { apiKeyStorage } from '@/lib/api'

export const Route = createFileRoute('/settings')({
  component: Settings,
})

function Settings() {
  const [modalOpen, setModalOpen] = useState(false)
  const [confirmText, setConfirmText] = useState('')
  const [toastVisible, setToastVisible] = useState(false)

  const clientId = apiKeyStorage.getClientId()
  const apiKey = apiKeyStorage.get()

  function copyClientId() {
    if (!clientId) return
    navigator.clipboard.writeText(clientId).then(() => {
      setToastVisible(true)
      setTimeout(() => setToastVisible(false), 3000)
    })
  }

  return (
    <div className="p-lg min-h-screen">
      <div className="max-w-5xl mx-auto space-y-lg">
        {/* Header */}
        <div className="flex flex-col gap-xs mb-xl">
          <h2 className="text-display font-display text-on-surface">API Credentials</h2>
          <p className="text-body-md text-on-surface-variant">Manage your secure access keys to integrate Notify with your backend systems.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-lg">
          {/* Left */}
          <div className="lg:col-span-8 space-y-lg">
            {/* Client ID */}
            <div className="tonal-layer-1 p-lg rounded-xl flex flex-col gap-md">
              <div className="flex items-center justify-between">
                <h3 className="text-headline-md font-headline-md text-on-surface">Client ID</h3>
                <span className="bg-primary-container text-on-primary-container text-label-sm px-sm py-1 rounded">Read-only</span>
              </div>
              <div className="flex items-center gap-sm bg-surface-container-lowest border border-outline-variant p-sm rounded group">
                <code className="font-mono text-mono text-primary flex-grow">
                  {clientId ?? <span className="text-outline italic">No client registered yet</span>}
                </code>
                {clientId && (
                  <button
                    className="material-symbols-outlined text-on-surface-variant hover:text-primary transition-colors p-1"
                    onClick={copyClientId}
                  >
                    content_copy
                  </button>
                )}
              </div>
              <p className="text-body-sm text-on-surface-variant">
                Use this ID to identify your application when making requests to the Notify API.
              </p>
            </div>

            {/* Active Keys Table */}
            <div className="tonal-layer-1 rounded-xl overflow-hidden">
              <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between">
                <h3 className="text-headline-md font-headline-md text-on-surface">Active Keys</h3>
                <span className="text-label-md text-on-surface-variant">{apiKey ? '1 Key Active' : 'No Keys'}</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead className="bg-surface-container-high">
                    <tr>
                      {['Client ID', 'API Key (masked)', 'Status'].map((h) => (
                        <th key={h} className="px-lg py-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant">
                    {apiKey ? (
                      <tr className="hover:bg-surface-container-high transition-colors">
                        <td className="px-lg py-md">
                          <div className="flex items-center gap-sm">
                            <span className="material-symbols-outlined text-tertiary">key</span>
                            <span className="text-body-md font-mono text-on-surface">{clientId ?? '—'}</span>
                          </div>
                        </td>
                        <td className="px-lg py-md font-mono text-xs text-on-surface-variant">
                          {apiKey.slice(0, 8)}{'•'.repeat(12)}{apiKey.slice(-4)}
                        </td>
                        <td className="px-lg py-md">
                          <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[10px] font-bold border border-emerald-500/20 uppercase tracking-tighter">
                            Active
                          </span>
                        </td>
                      </tr>
                    ) : (
                      <tr>
                        <td colSpan={3} className="px-lg py-xl text-center text-on-surface-variant text-body-sm italic">
                          No API key registered.{' '}
                          <Link to="/api-keys" className="text-primary underline not-italic">Register a client</Link> to get started.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Register New Client */}
            <Link
              to="/api-keys"
              className="flex items-center gap-sm bg-primary-container text-on-primary-container px-lg py-sm rounded border border-outline-variant hover:bg-surface-variant transition-colors text-label-md w-fit"
            >
              <span className="material-symbols-outlined text-[18px]">app_registration</span>
              Register New Client
            </Link>
          </div>

          {/* Right */}
          <div className="lg:col-span-4 space-y-lg">
            {/* Danger Zone */}
            <div className="tonal-layer-1 p-lg rounded-xl border-l-4 border-error flex flex-col gap-md">
              <h3 className="text-headline-md font-headline-md text-error flex items-center gap-sm">
                <span className="material-symbols-outlined">warning</span> Critical Action
              </h3>
              <p className="text-body-sm text-on-surface-variant leading-relaxed">
                Regenerating your primary secret will immediately invalidate the current key. Any services using the old key will start failing.
              </p>
              <button
                className="w-full py-md px-lg bg-error-container text-on-error-container hover:bg-error hover:text-on-error transition-all rounded-lg text-label-md font-bold flex items-center justify-center gap-sm"
                onClick={() => setModalOpen(true)}
              >
                <span className="material-symbols-outlined text-[18px]">refresh</span>
                Regenerate Primary Key
              </button>
            </div>

            {/* Security Info */}
            <div className="bg-tertiary-container text-on-tertiary-container p-lg rounded-xl border border-tertiary/20">
              <h4 className="text-label-md font-label-md font-bold uppercase tracking-widest mb-sm text-tertiary">Security Protocol</h4>
              <ul className="space-y-sm">
                {[
                  { icon: 'shield', text: 'Keys are encrypted at rest using AES-256.' },
                  { icon: 'history', text: 'All key access is logged in the Audit Trail.' },
                ].map((item) => (
                  <li key={item.icon} className="flex items-start gap-sm">
                    <span className="material-symbols-outlined text-[16px] mt-1">{item.icon}</span>
                    <span className="text-body-sm">{item.text}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="relative h-48 rounded-xl overflow-hidden border border-outline-variant bg-surface-container-high flex items-center justify-center">
              <span className="text-label-sm text-primary uppercase tracking-tighter">System Pulse Active</span>
            </div>
          </div>
        </div>
      </div>

      {/* Danger Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-md">
          <div className="absolute inset-0 bg-background/80 backdrop-blur-sm" onClick={() => setModalOpen(false)} />
          <div className="relative tonal-layer-2 rounded-xl shadow-2xl p-xl flex flex-col gap-lg border-t-4 border-error">
            <div className="flex flex-col gap-sm">
              <h2 className="text-headline-md font-headline-md text-on-surface">Are you absolutely sure?</h2>
              <p className="text-body-md text-on-surface-variant">
                This action is irreversible. All current integrations using{' '}
                <span className="font-mono text-error font-bold italic">Production_Main</span>{' '}
                will break until you update them with the new key.
              </p>
            </div>
            <div className="flex flex-col gap-sm">
              <label className="text-label-sm text-on-surface-variant">
                Type <span className="text-on-surface font-bold">REGENERATE</span> to confirm
              </label>
              <input
                className="bg-surface-container-lowest border border-outline-variant rounded-lg p-md font-mono text-on-surface outline-none focus:border-error transition-all"
                placeholder="REGENERATE"
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-md">
              <button
                className="flex-1 py-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors"
                onClick={() => { setModalOpen(false); setConfirmText('') }}
              >
                Cancel
              </button>
              <button
                className={`flex-1 py-md bg-error text-on-error font-bold rounded-lg text-label-md hover:opacity-90 transition-opacity ${confirmText !== 'REGENERATE' ? 'opacity-40 cursor-not-allowed' : ''
                  }`}
                disabled={confirmText !== 'REGENERATE'}
              >
                Regenerate Now
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      <div className={`fixed bottom-lg right-lg z-[60] transition-all duration-300 ${toastVisible ? 'translate-y-0 opacity-100' : 'translate-y-20 opacity-0'}`}>
        <div className="bg-surface-container-high border border-outline-variant rounded-lg p-md flex items-center gap-md shadow-xl border-l-4 border-primary">
          <span className="material-symbols-outlined text-primary">check_circle</span>
          <span className="text-body-md text-on-surface">Client ID copied to clipboard</span>
        </div>
      </div>
    </div>
  )
}
