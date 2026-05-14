import { NavLink, Outlet } from 'react-router-dom'

const adminNavItems = [
  { to: '/admin/dashboard', label: '概览' },
  { to: '/admin/chat-records', label: '问答日志' },
  { to: '/admin/feedback', label: '用户反馈' },
  { to: '/admin/unmatched', label: '未命中问题' },
]

export function AdminLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex min-h-16 max-w-7xl flex-wrap items-center justify-between gap-3 px-5 py-3">
          <div>
            <div className="text-base font-semibold">管理员后台</div>
            <div className="text-xs text-slate-500">AI 知识库问答平台运行概览</div>
          </div>
          <nav className="flex flex-wrap items-center gap-2 text-sm">
            {adminNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  ['app-nav-link', isActive ? 'app-nav-link-active' : 'app-nav-link-inactive'].join(' ')
                }
              >
                {item.label}
              </NavLink>
            ))}
            <NavLink
              to="/knowledge-bases"
              className="rounded border border-slate-300 px-3 py-2 text-slate-700 transition hover:bg-slate-100"
            >
              返回用户端
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl px-5 py-8">
        <Outlet />
      </main>
    </div>
  )
}
