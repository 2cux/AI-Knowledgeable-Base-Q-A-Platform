import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'

type LocationState = {
  from?: {
    pathname?: string
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response
    return response?.data?.message || fallback
  }

  if (error instanceof Error) {
    return error.message || fallback
  }

  return fallback
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, loading, login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const from = (location.state as LocationState | null)?.from?.pathname ?? '/chat'
  const redirectTo = from === '/login' || from === '/register' ? '/chat' : from

  useEffect(() => {
    if (!loading && isAuthenticated) {
      navigate('/chat', { replace: true })
    }
  }, [isAuthenticated, loading, navigate])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedUsername = username.trim()
    const normalizedPassword = password.trim()

    if (!normalizedUsername) {
      setErrorMessage('请输入用户名')
      return
    }

    if (!normalizedPassword) {
      setErrorMessage('请输入密码')
      return
    }

    setErrorMessage('')
    setIsSubmitting(true)

    try {
      const response = await loginApi({
        username: normalizedUsername,
        password: normalizedPassword,
      })

      if (response.code !== 0 || !response.data) {
        setErrorMessage(response.message || '登录失败，请稍后重试')
        return
      }

      await login(response.data)
      navigate(redirectTo, { replace: true })
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '登录失败，请检查用户名和密码'))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-100 text-sm text-slate-600">
        加载中...
      </main>
    )
  }

  return (
    <main className="min-h-screen bg-slate-100 px-5 py-10 text-slate-900">
      <section className="mx-auto mt-8 w-full max-w-md rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <div>
          <p className="text-sm font-medium text-slate-500">AI Knowledge Base QA</p>
          <h1 className="mt-2 text-2xl font-semibold">登录</h1>
          <p className="mt-2 text-sm text-slate-600">使用账号密码进入平台。</p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <label className="block">
            <span className="text-sm font-medium text-slate-700">用户名</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
              autoComplete="username"
              placeholder="alice"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-slate-700">密码</span>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
              type="password"
              autoComplete="current-password"
              placeholder="请输入密码"
            />
          </label>

          {errorMessage ? (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {errorMessage}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? '登录中...' : '登录'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          还没有账号？
          <Link to="/register" className="font-medium text-slate-900 hover:underline">
            去注册
          </Link>
        </p>
      </section>
    </main>
  )
}
