import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '../context/AuthContext'

type ProtectedRouteProps = {
  requiredRole?: string
}

export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const location = useLocation()
  const { isAuthenticated, loading, user } = useAuth()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm text-slate-600">
        加载中...
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requiredRole && user?.role?.toUpperCase() !== requiredRole.toUpperCase()) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 px-5 text-center">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">无权限访问管理后台</h1>
          <p className="mt-2 text-sm text-slate-600">当前账号不是管理员，请返回用户端继续使用。</p>
          <Link
            to="/chat"
            className="mt-4 inline-flex h-9 items-center rounded border border-slate-300 px-3 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            返回用户端
          </Link>
        </div>
      </div>
    )
  }

  return <Outlet />
}
