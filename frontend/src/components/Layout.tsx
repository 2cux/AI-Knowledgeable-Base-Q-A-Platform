import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/kb', label: '知识库' },
  { to: '/chat', label: '问答' },
  { to: '/admin', label: '管理' },
]

export function Layout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
          <NavLink to="/kb" className="text-base font-semibold">
            AI Knowledge Base QA
          </NavLink>
          <nav className="flex items-center gap-2 text-sm">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  [
                    'rounded px-3 py-2 transition',
                    isActive
                      ? 'bg-slate-900 text-white'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                  ].join(' ')
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-5 py-8">
        <Outlet />
      </main>
    </div>
  )
}
