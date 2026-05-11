import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { getToken } from '../utils/token'

export function ProtectedRoute() {
  const location = useLocation()
  const token = getToken()

  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}
