import { useEffect, useState } from 'react'

import { getAdminChatRecordDetail, getAdminUnmatchedQuestions } from '../../api/admin'
import type { AdminChatRecordDetail, AdminPage, AdminUnmatchedQuestion } from '../../types/admin'

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

export function AdminUnmatchedQuestionsPage() {
  const [pageData, setPageData] = useState<AdminPage<AdminUnmatchedQuestion>>({
    records: [],
    total: 0,
    page: 1,
    pageSize: PAGE_SIZE,
  })
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [knowledgeBaseId, setKnowledgeBaseId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [detail, setDetail] = useState<AdminChatRecordDetail | null>(null)

  useEffect(() => {
    let ignore = false

    async function loadQuestions() {
      setLoading(true)
      setError('')
      try {
        const data = await getAdminUnmatchedQuestions({ page, pageSize: PAGE_SIZE, keyword, knowledgeBaseId })
        if (!ignore) setPageData(data)
      } catch (loadError) {
        if (!ignore) setError(toErrorMessage(loadError))
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    void loadQuestions()
    return () => {
      ignore = true
    }
  }, [keyword, knowledgeBaseId, page])

  async function openDetail(id: number) {
    try {
      setDetail(await getAdminChatRecordDetail(id))
    } catch (loadError) {
      setError(toErrorMessage(loadError))
    }
  }

  const totalPages = Math.max(1, Math.ceil(pageData.total / PAGE_SIZE))

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-2xl font-semibold">未命中问题管理</h1>
        <p className="mt-1 text-sm text-slate-600">查看未检索到有效上下文或回答状态异常的问题。</p>
      </div>

      <form
        className="grid gap-3 rounded border border-slate-200 bg-white p-4 md:grid-cols-3"
        onSubmit={(event) => {
          event.preventDefault()
          setPage(1)
        }}
      >
        <input className="rounded border border-slate-300 px-3 py-2 text-sm" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="关键词" />
        <input className="rounded border border-slate-300 px-3 py-2 text-sm" value={knowledgeBaseId} onChange={(event) => setKnowledgeBaseId(event.target.value)} placeholder="知识库 ID" />
        <button className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white" type="submit">筛选</button>
      </form>

      {loading ? <div className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">加载中...</div> : null}
      {error ? <div className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      {!loading && !error ? (
        <div className="rounded border border-slate-200 bg-white">
          {pageData.records.length === 0 ? (
            <div className="p-6 text-sm text-slate-500">暂无未命中问题。</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">问题</th>
                    <th className="px-4 py-3">状态</th>
                    <th className="px-4 py-3">命中</th>
                    <th className="px-4 py-3">切片</th>
                    <th className="px-4 py-3">知识库</th>
                    <th className="px-4 py-3">时间</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pageData.records.map((item) => (
                    <tr key={item.id} className="align-top">
                      <td className="max-w-xl px-4 py-3">
                        <button type="button" className="text-left font-medium text-slate-900 hover:underline" onClick={() => void openDetail(item.id)}>
                          {item.question || '-'}
                        </button>
                      </td>
                      <td className="px-4 py-3">{item.answerStatus || '-'}</td>
                      <td className="px-4 py-3">{item.matched ? '是' : '否'}</td>
                      <td className="px-4 py-3">{item.retrievedChunkCount ?? 0}</td>
                      <td className="px-4 py-3">{item.knowledgeBaseId ?? '全企业知识库'}</td>
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

      {detail ? (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-slate-950/40 px-4">
          <div className="max-h-[80vh] w-full max-w-2xl overflow-y-auto rounded bg-white p-5 shadow-xl">
            <div className="flex items-center justify-between gap-4">
              <h2 className="text-lg font-semibold">未命中详情</h2>
              <button className="rounded border border-slate-300 px-3 py-1.5 text-sm" type="button" onClick={() => setDetail(null)}>关闭</button>
            </div>
            <div className="mt-4 space-y-4 text-sm">
              <div><div className="font-medium">问题</div><p className="mt-1 whitespace-pre-wrap text-slate-700">{detail.question || '-'}</p></div>
              <div><div className="font-medium">答案</div><p className="mt-1 whitespace-pre-wrap text-slate-700">{detail.answer || '-'}</p></div>
              <div className="grid gap-3 sm:grid-cols-3">
                <div>状态：{detail.answerStatus || '-'}</div>
                <div>有效切片：{detail.retrievedChunkCount ?? 0}</div>
                <div>原始切片：{detail.rawRetrievedChunkCount ?? 0}</div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
