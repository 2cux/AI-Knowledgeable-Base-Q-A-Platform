import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { register } from '../api/auth'
import { getToken } from '../utils/token'

function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response
    return response?.data?.message || fallback
  }

  return fallback
}

export function RegisterPage() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (getToken()) {
      navigate('/knowledge-bases', { replace: true })
    }
  }, [navigate])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalizedUsername = username.trim()
    const normalizedEmail = email.trim()
    const normalizedPassword = password.trim()
    const normalizedConfirmPassword = confirmPassword.trim()

    if (!normalizedUsername) {
      setErrorMessage('请输入用户名')
      return
    }

    if (!normalizedEmail) {
      setErrorMessage('请输入邮箱')
      return
    }

    if (!normalizedPassword) {
      setErrorMessage('请输入密码')
      return
    }

    if (normalizedPassword !== normalizedConfirmPassword) {
      setErrorMessage('两次输入的密码不一致')
      return
    }

    setErrorMessage('')
    setIsSubmitting(true)

    try {
      const response = await register({
        username: normalizedUsername,
        email: normalizedEmail,
        password: normalizedPassword,
        confirmPassword: normalizedConfirmPassword,
      })

      if (response.code !== 0) {
        setErrorMessage(response.message || '注册失败，请稍后重试')
        return
      }

      navigate('/login', {
        replace: true,
        state: { registered: true },
      })
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '注册失败，请检查输入信息'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-100 px-5 py-10 text-slate-900">
      <section className="mx-auto mt-8 w-full max-w-md rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <div>
          <p className="text-sm font-medium text-slate-500">AI Knowledge Base QA</p>
          <h1 className="mt-2 text-2xl font-semibold">注册</h1>
          <p className="mt-2 text-sm text-slate-600">创建账号后返回登录页使用平台。</p>
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
            <span className="text-sm font-medium text-slate-700">邮箱</span>
            <input
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
              type="email"
              autoComplete="email"
              placeholder="alice@example.com"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-slate-700">密码</span>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
              type="password"
              autoComplete="new-password"
              placeholder="请输入密码"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-slate-700">确认密码</span>
            <input
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
              type="password"
              autoComplete="new-password"
              placeholder="再次输入密码"
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
            {isSubmitting ? '注册中...' : '注册'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          已有账号？
          <Link to="/login" className="font-medium text-slate-900 hover:underline">
            去登录
          </Link>
        </p>
      </section>
    </main>
  )
}
