import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { saveToken } from '../utils/token'

type LocationState = {
  from?: {
    pathname?: string
  }
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as LocationState | null)?.from?.pathname ?? '/kb'

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const token = String(formData.get('token') ?? '').trim()

    if (token) {
      saveToken(token)
      navigate(from, { replace: true })
    }
  }

  return (
    <section className="mx-auto max-w-md">
      <h1 className="text-2xl font-semibold">登录</h1>
      <p className="mt-2 text-sm text-slate-600">请输入后端登录接口返回的 token。</p>
      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <label className="block">
          <span className="text-sm font-medium text-slate-700">Token</span>
          <input
            name="token"
            className="mt-2 w-full rounded border border-slate-300 bg-white px-3 py-2 outline-none focus:border-slate-900"
            placeholder="Bearer token"
          />
        </label>
        <button className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700">
          进入系统
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        还没有账号？<Link to="/register" className="font-medium text-slate-900">去注册</Link>
      </p>
    </section>
  )
}
