import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { getCurrentUser } from '../api/auth'
import { UNAUTHORIZED_EVENT } from '../api/request'
import type { CurrentUser, LoginResponse } from '../types/auth'
import { getToken, removeToken, saveToken } from '../utils/token'

type AuthContextValue = {
  user: CurrentUser | null
  token: string | null
  loading: boolean
  isAuthenticated: boolean
  login: (loginResponse: LoginResponse) => Promise<CurrentUser>
  logout: () => void
  refreshUser: () => Promise<CurrentUser | null>
}

const AuthContext = createContext<AuthContextValue | null>(null)

type AuthProviderProps = {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [token, setToken] = useState<string | null>(() => getToken())
  const [loading, setLoading] = useState(true)

  const clearAuth = useCallback(() => {
    removeToken()
    setToken(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const currentToken = getToken()

    if (!currentToken) {
      clearAuth()
      return null
    }

    try {
      const response = await getCurrentUser()

      if (response.code !== 0 || !response.data) {
        throw new Error(response.message || '获取当前用户失败')
      }

      setToken(currentToken)
      setUser(response.data)
      return response.data
    } catch (error) {
      clearAuth()
      throw error
    }
  }, [clearAuth])

  const login = useCallback(
    async (loginResponse: LoginResponse) => {
      if (!loginResponse.token) {
        throw new Error('登录响应缺少 token')
      }

      saveToken(loginResponse.token)
      setToken(loginResponse.token)

      try {
        const currentUser = await refreshUser()

        if (!currentUser) {
          throw new Error('获取当前用户失败')
        }

        return currentUser
      } catch (error) {
        clearAuth()
        throw error
      }
    },
    [clearAuth, refreshUser],
  )

  const logout = useCallback(() => {
    clearAuth()
  }, [clearAuth])

  useEffect(() => {
    let ignore = false

    async function restoreAuth() {
      if (!getToken()) {
        clearAuth()
        if (!ignore) {
          setLoading(false)
        }
        return
      }

      try {
        await refreshUser()
      } catch {
        clearAuth()
      } finally {
        if (!ignore) {
          setLoading(false)
        }
      }
    }

    void restoreAuth()

    return () => {
      ignore = true
    }
  }, [clearAuth, refreshUser])

  useEffect(() => {
    function handleUnauthorized() {
      clearAuth()
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => {
      window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    }
  }, [clearAuth])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      loading,
      isAuthenticated: Boolean(token && user),
      login,
      logout,
      refreshUser,
    }),
    [loading, login, logout, refreshUser, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
