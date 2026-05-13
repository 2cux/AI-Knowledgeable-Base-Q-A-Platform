import { NavLink, useNavigate } from 'react-router-dom'

import { useAuth } from '../context/AuthContext'

const baseNavItems = [
  { to: '/knowledge-bases', label: '知识库' },
  { to: '/chat', label: '问答' },
]

export function Navbar() {
  const navigate = useNavigate()
  const { logout, user } = useAuth()
  const navItems =
    user?.role?.toUpperCase() === 'ADMIN'
      ? [...baseNavItems, { to: '/admin', label: '管理' }]
      : baseNavItems

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-5 px-5">
        <NavLink to="/knowledge-bases" className="shrink-0 text-base font-semibold">
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

        <div className="flex shrink-0 items-center gap-3 text-sm">
          <span className="max-w-32 truncate text-slate-600">{user?.username ?? '用户'}</span>
          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex h-9 items-center gap-1.5 rounded border border-slate-300 px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
            title="退出登录"
          >
            <span>退出</span>
          </button>
        </div>
      </div>
    </header>
  )
}
