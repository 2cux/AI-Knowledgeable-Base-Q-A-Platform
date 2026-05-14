import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../context/AuthContext'

export function UserMenu() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [open])

  function handleToggle() {
    setOpen((prev) => !prev)
  }

  function handleLogout() {
    setOpen(false)
    logout()
    navigate('/login', { replace: true })
  }

  function handleGoAdmin() {
    setOpen(false)
    navigate('/admin', { replace: true })
  }

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN'
  const initial = user?.username?.charAt(0)?.toUpperCase() ?? 'U'

  return (
    <div ref={menuRef} className="relative shrink-0 border-t border-slate-200">
      <button
        type="button"
        onClick={handleToggle}
        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition hover:bg-slate-50"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-900 text-xs font-medium text-white">
          {initial}
        </span>
        <span className="min-w-0 flex-1 truncate text-slate-700">
          {user?.username ?? '用户'}
        </span>
      </button>

      {open ? (
        <div className="absolute bottom-full left-2 right-2 z-50 mb-1 rounded-md border border-slate-200 bg-white py-1 shadow-lg">
          <div className="border-b border-slate-100 px-3 py-2">
            <div className="text-sm font-medium text-slate-900">{user?.username ?? '用户'}</div>
            {user?.role ? (
              <div className="mt-0.5 text-xs text-slate-500">{isAdmin ? '管理员' : '普通用户'}</div>
            ) : null}
          </div>

          {isAdmin ? (
            <button
              type="button"
              onClick={handleGoAdmin}
              className="flex w-full items-center px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            >
              进入管理员后台
            </button>
          ) : null}

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
          >
            退出登录
          </button>
        </div>
      ) : null}
    </div>
  )
}
