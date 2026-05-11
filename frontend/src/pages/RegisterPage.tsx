import { Link } from 'react-router-dom'

export function RegisterPage() {
  return (
    <section>
      <h1 className="text-2xl font-semibold">注册</h1>
      <p className="mt-2 text-slate-600">注册页面占位，后续接入用户注册接口。</p>
      <Link to="/login" className="mt-6 inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
        返回登录
      </Link>
    </section>
  )
}
