import { Link, useRouterState } from '@tanstack/react-router'
import type { ReactNode } from 'react'

const navItems = [
  { label: 'Dashboard', icon: 'dashboard', to: '/dashboard' },
  { label: 'Campaigns', icon: 'campaign', to: '/campaigns' },
  { label: 'Send', icon: 'send', to: '/send' },
  { label: 'Bulk Upload', icon: 'upload_file', to: '/bulk-upload' },
  { label: 'Cohorts', icon: 'groups', to: '/cohorts' },
  { label: 'DLQ', icon: 'error_outline', to: '/dlq' },
  { label: 'Settings', icon: 'settings', to: '/settings' },
]

export default function Layout({ children }: { children: ReactNode }) {
  const { location } = useRouterState()

  function isActive(to: string) {
    if (to === '/settings') {
      return location.pathname === '/settings' || location.pathname === '/api-keys'
    }
    return location.pathname === to
  }

  return (
    <div className="min-h-screen bg-surface-container-lowest font-body-md text-on-surface">
      {/* Top App Bar */}
      <header className="fixed top-0 w-full h-16 z-40 bg-surface-container-low border-b border-outline-variant flex items-center justify-between px-lg">
        <div className="flex items-center gap-sm">
          <span className="material-symbols-outlined text-primary">notifications_active</span>
          <h1 className="text-headline-lg font-headline-lg font-bold text-primary tracking-tight">Notify</h1>
        </div>
        <div className="flex items-center gap-md">
          <div className="flex items-center gap-xs text-on-surface-variant text-label-md">
            <span className="material-symbols-outlined text-[18px]">bolt</span>
            <span>System Health: 99.9%</span>
          </div>
          <div className="h-6 w-px bg-outline-variant" />
          <div className="px-sm py-xs bg-primary-container text-on-primary-container rounded text-label-sm tracking-widest">
            PROD
          </div>
        </div>
      </header>

      {/* Sidebar */}
      <aside className="fixed left-0 top-16 h-[calc(100vh-64px)] w-[240px] z-30 bg-surface-container-low border-r border-outline-variant flex flex-col p-sm">
        {/* User info */}
        <div className="flex items-center gap-sm px-sm py-md mb-sm border-b border-outline-variant">
          <div className="w-8 h-8 rounded-lg bg-primary-container border border-outline-variant flex items-center justify-center text-on-surface text-label-sm font-bold flex-shrink-0">
            DN
          </div>
          <div className="overflow-hidden">
            <p className="text-body-sm font-semibold text-on-surface truncate">Developer Name</p>
            <p className="text-label-sm text-on-surface-variant">Admin Access</p>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex flex-col gap-0.5 mt-sm">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={[
                'flex items-center gap-sm px-sm py-2.5 rounded-lg transition-all duration-200 text-body-sm',
                isActive(item.to)
                  ? 'bg-secondary-container text-on-secondary-container font-semibold'
                  : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface',
              ].join(' ')}
            >
              <span className={['material-symbols-outlined text-[20px]', isActive(item.to) ? '[font-variation-settings:"FILL"_1,"wght"_400,"GRAD"_0,"opsz"_20]' : ''].join(' ')}>{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          ))}
        </nav>

        {/* Bottom spacer with version */}
        <div className="mt-auto px-sm pb-sm">
          <div className="text-[10px] text-outline tracking-widest uppercase">v2.4.0-release</div>
        </div>
      </aside>

      {/* Main content */}
      <main className="ml-[240px] pt-16 min-h-screen">
        {children}
      </main>
    </div>
  )
}
