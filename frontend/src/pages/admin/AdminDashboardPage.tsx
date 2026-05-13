import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { getAdminDashboard } from '../../api/admin'
import type { AdminDashboard } from '../../types/admin'

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function toErrorMessage(error: unknown) {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { status?: number; data?: { message?: string } } }).response
    if (response?.status === 403) return '无权限访问管理接口'
    if (response?.status === 500) return '服务异常，请稍后重试'
    return response?.data?.message || '请求失败'
  }
  return error instanceof Error ? error.message : '请求失败'
}

export function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    async function loadDashboard() {
      setLoading(true)
      setError('')
      try {
        const data = await getAdminDashboard()
        if (!ignore) setDashboard(data)
      } catch (loadError) {
        if (!ignore) setError(toErrorMessage(loadError))
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    void loadDashboard()
    return () => {
      ignore = true
    }
  }, [])

  const stats = dashboard
    ? [
        { label: '知识库数量', value: dashboard.knowledgeBaseCount },
        { label: '文档数量', value: dashboard.documentCount },
        { label: '问答数量', value: dashboard.chatRecordCount },
        { label: '反馈数量', value: dashboard.feedbackCount },
        { label: '未命中数量', value: dashboard.unmatchedQuestionCount },
      ]
    : []

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">管理端概览</h1>
        <p className="mt-1 text-sm text-slate-600">查看系统基础统计和最近问答情况。</p>
      </div>

      {loading ? <div className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">加载中...</div> : null}
      {error ? <div className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      {!loading && !error && dashboard ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {stats.map((item) => (
              <div key={item.label} className="rounded border border-slate-200 bg-white p-4">
                <div className="text-sm text-slate-500">{item.label}</div>
                <div className="mt-2 text-2xl font-semibold text-slate-900">{item.value}</div>
              </div>
            ))}
          </div>

          <div className="rounded border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
              <h2 className="font-semibold">最近问答日志</h2>
              <Link to="/admin/chat-records" className="text-sm font-medium text-slate-700 hover:text-slate-950">
                查看全部
              </Link>
            </div>
            {dashboard.recentChatRecords.length === 0 ? (
              <div className="p-6 text-sm text-slate-500">暂无问答日志。</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">问题</th>
                      <th className="px-4 py-3">状态</th>
                      <th className="px-4 py-3">命中</th>
                      <th className="px-4 py-3">时间</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {dashboard.recentChatRecords.map((record) => (
                      <tr key={record.id} className="align-top">
                        <td className="max-w-xl px-4 py-3">
                          <Link
                            to={`/admin/chat-records?keyword=${encodeURIComponent(record.question)}`}
                            className="font-medium text-slate-900 hover:underline"
                          >
                            {record.question || '-'}
                          </Link>
                        </td>
                        <td className="px-4 py-3">{record.answerStatus || '-'}</td>
                        <td className="px-4 py-3">{record.matched ? '是' : '否'}</td>
                        <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatTime(record.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : null}
    </section>
  )
}
