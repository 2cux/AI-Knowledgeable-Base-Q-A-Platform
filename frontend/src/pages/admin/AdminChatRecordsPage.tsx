import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { getAdminChatRecordDetail, getAdminChatRecords } from '../../api/admin'
import type { AdminChatRecordDetail, AdminChatRecordListItem, AdminPage } from '../../types/admin'

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

export function AdminChatRecordsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [pageData, setPageData] = useState<AdminPage<AdminChatRecordListItem>>({
    records: [],
    total: 0,
    page: Number(searchParams.get('page') || 1),
    pageSize: PAGE_SIZE,
  })
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [knowledgeBaseId, setKnowledgeBaseId] = useState(searchParams.get('knowledgeBaseId') || '')
  const [matched, setMatched] = useState(searchParams.get('matched') || '')
  const [answerStatus, setAnswerStatus] = useState(searchParams.get('answerStatus') || '')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [detail, setDetail] = useState<AdminChatRecordDetail | null>(null)
  const [detailError, setDetailError] = useState('')

  const page = Number(searchParams.get('page') || 1)

  useEffect(() => {
    let ignore = false

    async function loadRecords() {
      setLoading(true)
      setError('')
      try {
        const data = await getAdminChatRecords({
          page,
          pageSize: PAGE_SIZE,
          keyword: searchParams.get('keyword') || '',
          knowledgeBaseId: searchParams.get('knowledgeBaseId') || '',
          matched: searchParams.get('matched') || '',
          answerStatus: searchParams.get('answerStatus') || '',
        })
        if (!ignore) setPageData(data)
      } catch (loadError) {
        if (!ignore) setError(toErrorMessage(loadError))
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    void loadRecords()
    return () => {
      ignore = true
    }
  }, [page, searchParams])

  function applyFilters(nextPage = 1) {
    const next = new URLSearchParams()
    if (keyword.trim()) next.set('keyword', keyword.trim())
    if (knowledgeBaseId.trim()) next.set('knowledgeBaseId', knowledgeBaseId.trim())
    if (matched) next.set('matched', matched)
    if (answerStatus) next.set('answerStatus', answerStatus)
    if (nextPage > 1) next.set('page', String(nextPage))
    setSearchParams(next)
  }

  async function openDetail(id: number) {
    setDetail(null)
    setDetailError('')
    try {
      setDetail(await getAdminChatRecordDetail(id))
    } catch (loadError) {
      setDetailError(toErrorMessage(loadError))
    }
  }

  const totalPages = Math.max(1, Math.ceil(pageData.total / PAGE_SIZE))

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-2xl font-semibold">问答日志管理</h1>
        <p className="mt-1 text-sm text-slate-600">按问题、知识库、命中状态和回答状态筛选问答记录。</p>
      </div>

      <form
        className="grid gap-3 rounded border border-slate-200 bg-white p-4 md:grid-cols-5"
        onSubmit={(event) => {
          event.preventDefault()
          applyFilters()
        }}
      >
        <input className="rounded border border-slate-300 px-3 py-2 text-sm" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="关键词" />
        <input className="rounded border border-slate-300 px-3 py-2 text-sm" value={knowledgeBaseId} onChange={(event) => setKnowledgeBaseId(event.target.value)} placeholder="知识库 ID" />
        <select className="rounded border border-slate-300 px-3 py-2 text-sm" value={matched} onChange={(event) => setMatched(event.target.value)}>
          <option value="">命中状态</option>
          <option value="true">已命中</option>
          <option value="false">未命中</option>
        </select>
        <select className="rounded border border-slate-300 px-3 py-2 text-sm" value={answerStatus} onChange={(event) => setAnswerStatus(event.target.value)}>
          <option value="">回答状态</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="NO_HIT">NO_HIT</option>
          <option value="WEAK_HIT">WEAK_HIT</option>
          <option value="LLM_UNAVAILABLE">LLM_UNAVAILABLE</option>
          <option value="RETRIEVAL_UNAVAILABLE">RETRIEVAL_UNAVAILABLE</option>
        </select>
        <button className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white" type="submit">筛选</button>
      </form>

      {loading ? <div className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">加载中...</div> : null}
      {error ? <div className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      {!loading && !error ? (
        <div className="rounded border border-slate-200 bg-white">
          {pageData.records.length === 0 ? (
            <div className="p-6 text-sm text-slate-500">暂无问答日志。</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">问题</th>
                    <th className="px-4 py-3">回答摘要</th>
                    <th className="px-4 py-3">状态</th>
                    <th className="px-4 py-3">命中</th>
                    <th className="px-4 py-3">切片</th>
                    <th className="px-4 py-3">知识库</th>
                    <th className="px-4 py-3">时间</th>
                    <th className="px-4 py-3">操作</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pageData.records.map((record) => (
                    <tr key={record.id} className="align-top">
                      <td className="max-w-sm px-4 py-3">{record.question || '-'}</td>
                      <td className="max-w-md px-4 py-3 text-slate-600">{record.answerPreview || '-'}</td>
                      <td className="px-4 py-3">{record.answerStatus || '-'}</td>
                      <td className="px-4 py-3">{record.matched ? '是' : '否'}</td>
                      <td className="px-4 py-3">{record.retrievedChunkCount ?? 0}</td>
                      <td className="px-4 py-3">{record.knowledgeBaseId}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatTime(record.createdAt)}</td>
                      <td className="px-4 py-3">
                        <button className="text-sm font-medium text-slate-900 hover:underline" type="button" onClick={() => void openDetail(record.id)}>
                          查看详情
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
            <span>共 {pageData.total} 条，第 {page} / {totalPages} 页</span>
            <div className="flex gap-2">
              <button className="rounded border border-slate-300 px-3 py-1.5 disabled:opacity-50" type="button" disabled={page <= 1} onClick={() => applyFilters(page - 1)}>上一页</button>
              <button className="rounded border border-slate-300 px-3 py-1.5 disabled:opacity-50" type="button" disabled={page >= totalPages} onClick={() => applyFilters(page + 1)}>下一页</button>
            </div>
          </div>
        </div>
      ) : null}

      {detail || detailError ? (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-slate-950/40 px-4">
          <div className="max-h-[85vh] w-full max-w-3xl overflow-y-auto rounded bg-white p-5 shadow-xl">
            <div className="flex items-center justify-between gap-4">
              <h2 className="text-lg font-semibold">问答详情</h2>
              <button className="rounded border border-slate-300 px-3 py-1.5 text-sm" type="button" onClick={() => { setDetail(null); setDetailError('') }}>关闭</button>
            </div>
            {detailError ? <div className="mt-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">{detailError}</div> : null}
            {detail ? (
              <div className="mt-4 space-y-4 text-sm">
                <div><div className="font-medium">问题</div><p className="mt-1 whitespace-pre-wrap text-slate-700">{detail.question || '-'}</p></div>
                <div><div className="font-medium">答案</div><p className="mt-1 whitespace-pre-wrap text-slate-700">{detail.answer || '-'}</p></div>
                <div className="grid gap-3 sm:grid-cols-4">
                  <div>状态：{detail.answerStatus || '-'}</div>
                  <div>命中：{detail.matched ? '是' : '否'}</div>
                  <div>有效切片：{detail.retrievedChunkCount ?? 0}</div>
                  <div>原始切片：{detail.rawRetrievedChunkCount ?? 0}</div>
                </div>
                <div>
                  <div className="font-medium">引用来源</div>
                  {detail.citations?.length ? (
                    <div className="mt-2 space-y-2">
                      {detail.citations.map((citation, index) => (
                        <div key={`${citation.documentName}-${citation.chunkIndex}-${index}`} className="rounded border border-slate-200 p-3">
                          <div className="text-slate-900">{citation.documentName || '未知文档'} · chunk {citation.chunkIndex ?? '-'}</div>
                          <div className="mt-1 text-xs text-slate-500">score: {citation.score ?? '-'}</div>
                          <p className="mt-2 whitespace-pre-wrap text-slate-700">{citation.contentSnippet || '-'}</p>
                        </div>
                      ))}
                    </div>
                  ) : <div className="mt-2 text-slate-500">无引用来源。</div>}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  )
}
