import { useEffect, useState } from 'react'

import { getAdminFeedback } from '../../api/admin'
import type { AdminFeedbackItem, AdminPage } from '../../types/admin'

const PAGE_SIZE = 20

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

export function AdminFeedbackPage() {
  const [pageData, setPageData] = useState<AdminPage<AdminFeedbackItem>>({
    records: [],
    total: 0,
    page: 1,
    pageSize: PAGE_SIZE,
  })
  const [page, setPage] = useState(1)
  const [feedbackType, setFeedbackType] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    async function loadFeedback() {
      setLoading(true)
      setError('')
      try {
        const data = await getAdminFeedback({ page, pageSize: PAGE_SIZE, feedbackType, reason })
        if (!ignore) setPageData(data)
      } catch (loadError) {
        if (!ignore) setError(toErrorMessage(loadError))
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    void loadFeedback()
    return () => {
      ignore = true
    }
  }, [feedbackType, page, reason])

  const totalPages = Math.max(1, Math.ceil(pageData.total / PAGE_SIZE))

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-2xl font-semibold">用户反馈管理</h1>
        <p className="mt-1 text-sm text-slate-600">查看用户对问答结果的反馈，当前版本仅展示已存储字段。</p>
      </div>

      <form
        className="grid gap-3 rounded border border-slate-200 bg-white p-4 md:grid-cols-3"
        onSubmit={(event) => {
          event.preventDefault()
          setPage(1)
        }}
      >
        <select className="rounded border border-slate-300 px-3 py-2 text-sm" value={feedbackType} onChange={(event) => { setFeedbackType(event.target.value); setPage(1) }}>
          <option value="">反馈类型</option>
          <option value="LIKE">LIKE</option>
          <option value="DISLIKE">DISLIKE</option>
        </select>
        <select className="rounded border border-slate-300 px-3 py-2 text-sm" value={reason} onChange={(event) => { setReason(event.target.value); setPage(1) }}>
          <option value="">反馈原因</option>
          <option value="ANSWER_ACCURATE">ANSWER_ACCURATE</option>
          <option value="ANSWER_INACCURATE">ANSWER_INACCURATE</option>
          <option value="SOURCE_NOT_RELEVANT">SOURCE_NOT_RELEVANT</option>
          <option value="ANSWER_INCOMPLETE">ANSWER_INCOMPLETE</option>
          <option value="HALLUCINATION">HALLUCINATION</option>
          <option value="FORMAT_BAD">FORMAT_BAD</option>
          <option value="OTHER">OTHER</option>
        </select>
        <button className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white" type="submit">筛选</button>
      </form>

      {loading ? <div className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">加载中...</div> : null}
      {error ? <div className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      {!loading && !error ? (
        <div className="rounded border border-slate-200 bg-white">
          {pageData.records.length === 0 ? (
            <div className="p-6 text-sm text-slate-500">暂无用户反馈。</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">类型</th>
                    <th className="px-4 py-3">原因</th>
                    <th className="px-4 py-3">备注</th>
                    <th className="px-4 py-3">知识库</th>
                    <th className="px-4 py-3">会话</th>
                    <th className="px-4 py-3">记录</th>
                    <th className="px-4 py-3">处理</th>
                    <th className="px-4 py-3">时间</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pageData.records.map((item) => (
                    <tr key={item.id} className="align-top">
                      <td className="px-4 py-3">{item.feedbackType || item.rating || '-'}</td>
                      <td className="px-4 py-3">{item.reason || '-'}</td>
                      <td className="max-w-md px-4 py-3 text-slate-700">{item.comment || '-'}</td>
                      <td className="px-4 py-3">{item.knowledgeBaseId ?? '-'}</td>
                      <td className="max-w-48 px-4 py-3 text-slate-600">{item.conversationId || '-'}</td>
                      <td className="px-4 py-3">{item.chatRecordId ?? item.messageId ?? '-'}</td>
                      <td className="px-4 py-3">{item.handled === null || item.handled === undefined ? '未支持' : item.handled ? '已处理' : '未处理'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatTime(item.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
            <span>共 {pageData.total} 条，第 {page} / {totalPages} 页</span>
            <div className="flex gap-2">
              <button className="rounded border border-slate-300 px-3 py-1.5 disabled:opacity-50" type="button" disabled={page <= 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>上一页</button>
              <button className="rounded border border-slate-300 px-3 py-1.5 disabled:opacity-50" type="button" disabled={page >= totalPages} onClick={() => setPage((value) => value + 1)}>下一页</button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
