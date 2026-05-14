import type { FormEvent } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  getKnowledgeBases,
  updateKnowledgeBase,
} from '../api/knowledgeBase'
import type { KnowledgeBase } from '../types/knowledgeBase'

const DEFAULT_PAGE_NUM = 1
const DEFAULT_PAGE_SIZE = 20

type FormState = {
  mode: 'create'
  name: string
  description: string
}

type EditFormState = {
  name: string
  description: string
}

type ResponseError = {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

function isResponseError(error: unknown): error is ResponseError {
  return typeof error === 'object' && error !== null && 'response' in error
}

function getErrorMessage(error: unknown, fallback: string) {
  if (isResponseError(error)) {
    const response = error.response
    return response?.data?.message || fallback
  }

  if (error instanceof Error) {
    return error.message || fallback
  }

  return fallback
}

function getDeleteErrorMessage(error: unknown) {
  if (isResponseError(error)) {
    const status = error.response?.status

    if (status === 401) {
      return '登录已失效，请重新登录后再删除知识库。'
    }

    if (status === 403) {
      return '无权限删除该知识库。'
    }

    if (status === 404) {
      return '知识库不存在或已被删除。'
    }

    if (status && status >= 500) {
      return '服务异常，请稍后重试。'
    }

    return error.response?.data?.message || '删除知识库失败，请稍后重试。'
  }

  if (error instanceof Error) {
    return error.message || '删除知识库失败，请稍后重试。'
  }

  return '删除知识库失败，请稍后重试。'
}

function getDeleteResponseMessage(code: number, message?: string) {
  if (code === 401 || code === 40100) {
    return '登录已失效，请重新登录后再删除知识库。'
  }

  if (code === 403 || code === 40300) {
    return '无权限删除该知识库。'
  }

  if (code === 404 || code === 40400) {
    return '知识库不存在或已被删除。'
  }

  if (code >= 500 || code === 50000) {
    return '服务异常，请稍后重试。'
  }

  return message || '删除知识库失败，请稍后重试。'
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

function createEmptyForm(): FormState {
  return {
    mode: 'create',
    name: '',
    description: '',
  }
}

export function KnowledgeBaseListPage() {
  const navigate = useNavigate()
  const savingRef = useRef(false)
  const savingEditRef = useRef(false)
  const deletingRef = useRef(false)

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
  const [total, setTotal] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [savingEdit, setSavingEdit] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [formErrorMessage, setFormErrorMessage] = useState('')
  const [formState, setFormState] = useState<FormState | null>(null)
  const [editingKnowledgeBase, setEditingKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [editForm, setEditForm] = useState<EditFormState>({ name: '', description: '' })
  const [editOpen, setEditOpen] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)
  const [pendingDeleteKnowledgeBase, setPendingDeleteKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('')

  async function loadKnowledgeBases() {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const response = await getKnowledgeBases({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
      })

      if (response.code !== 0 || !response.data) {
        setErrorMessage(response.message || '知识库列表加载失败')
        setKnowledgeBases([])
        setTotal(0)
        return
      }

      setKnowledgeBases(response.data.list ?? [])
      setTotal(response.data.total)
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '知识库列表加载失败，请稍后重试'))
      setKnowledgeBases([])
      setTotal(0)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadKnowledgeBases()
  }, [])

  function openCreateForm() {
    setErrorMessage('')
    setSuccessMessage('')
    setFormErrorMessage('')
    setFormState(createEmptyForm())
  }

  function openEditForm(knowledgeBase: KnowledgeBase) {
    setErrorMessage('')
    setSuccessMessage('')
    setEditError(null)
    setEditingKnowledgeBase(knowledgeBase)
    setEditForm({
      name: knowledgeBase.name,
      description: knowledgeBase.description ?? '',
    })
    setEditOpen(true)
  }

  function closeEditDialog() {
    if (savingEditRef.current) {
      return
    }

    setEditOpen(false)
    setEditingKnowledgeBase(null)
    setEditError(null)
    setEditForm({ name: '', description: '' })
  }

  function closeForm() {
    if (isSaving) {
      return
    }

    setFormErrorMessage('')
    setFormState(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!formState || savingRef.current) {
      return
    }

    const name = formState.name.trim()
    const description = formState.description.trim()

    if (!name) {
      setFormErrorMessage('请输入知识库名称')
      return
    }

    savingRef.current = true
    setIsSaving(true)
    setErrorMessage('')
    setSuccessMessage('')
    setFormErrorMessage('')

    try {
      // 统一在提交前 trim，再调用创建接口。
      const response = await createKnowledgeBase({ name, description })

      if (response.code !== 0 || !response.data) {
        setFormErrorMessage(response.message || '保存知识库失败')
        return
      }

      setFormState(null)
      setFormErrorMessage('')
      setSuccessMessage('知识库创建成功')
      await loadKnowledgeBases()
    } catch (error) {
      setFormErrorMessage(getErrorMessage(error, '保存知识库失败，请稍后重试'))
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  async function handleSaveEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!editingKnowledgeBase || savingEditRef.current) {
      return
    }

    const name = editForm.name.trim()
    const description = editForm.description.trim()

    if (!name) {
      setEditError('请输入知识库名称')
      return
    }

    savingEditRef.current = true
    setSavingEdit(true)
    setErrorMessage('')
    setSuccessMessage('')
    setEditError(null)

    try {
      const response = await updateKnowledgeBase(editingKnowledgeBase.id, { name, description })

      if (response.code !== 0 || !response.data) {
        setEditError(response.message || '保存知识库失败')
        return
      }

      setEditOpen(false)
      setEditingKnowledgeBase(null)
      setEditForm({ name: '', description: '' })
      setEditError(null)
      setSuccessMessage('知识库更新成功')
      await loadKnowledgeBases()
    } catch (error) {
      setEditError(getErrorMessage(error, '保存知识库失败，请稍后重试'))
    } finally {
      savingEditRef.current = false
      setSavingEdit(false)
    }
  }

  function openDeleteConfirm(knowledgeBase: KnowledgeBase) {
    if (deletingRef.current) {
      return
    }

    setErrorMessage('')
    setSuccessMessage('')
    setDeleteErrorMessage('')
    setPendingDeleteKnowledgeBase(knowledgeBase)
  }

  function closeDeleteConfirm() {
    if (deletingRef.current) {
      return
    }

    setPendingDeleteKnowledgeBase(null)
    setDeleteErrorMessage('')
  }

  async function confirmDeleteKnowledgeBase() {
    if (!pendingDeleteKnowledgeBase || deletingRef.current) {
      return
    }

    const knowledgeBase = pendingDeleteKnowledgeBase

    if (deletingRef.current) {
      return
    }

    deletingRef.current = true
    setDeletingId(knowledgeBase.id)
    setErrorMessage('')
    setSuccessMessage('')
    setDeleteErrorMessage('')

    try {
      const response = await deleteKnowledgeBase(knowledgeBase.id)

      if (response.code !== 0) {
        setDeleteErrorMessage(getDeleteResponseMessage(response.code, response.message))
        return
      }

      if (editingKnowledgeBase?.id === knowledgeBase.id) {
        setEditOpen(false)
        setEditingKnowledgeBase(null)
        setEditForm({ name: '', description: '' })
        setEditError(null)
      }
      setFormErrorMessage('')
      setSuccessMessage('知识库删除成功')
      setPendingDeleteKnowledgeBase(null)
      setDeleteErrorMessage('')
      await loadKnowledgeBases()
    } catch (error) {
      setDeleteErrorMessage(getDeleteErrorMessage(error))
    } finally {
      deletingRef.current = false
      setDeletingId(null)
    }
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">知识库管理</h1>
          <p className="mt-2 text-sm text-slate-600">管理当前账号下的知识库基础信息。</p>
        </div>
        <button
          type="button"
          onClick={openCreateForm}
          className="inline-flex h-10 items-center justify-center rounded bg-slate-900 px-4 text-sm font-medium text-white transition hover:bg-slate-700"
        >
          新建知识库
        </button>
      </div>

      {errorMessage ? (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      {successMessage ? (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {successMessage}
        </div>
      ) : null}

      {formState ? (
        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div>
            <h2 className="text-base font-semibold">
              新建知识库
            </h2>
            <p className="mt-1 text-sm text-slate-500">名称必填，描述可选。</p>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="text-sm font-medium text-slate-700">名称</span>
              <input
                value={formState.name}
                onChange={(event) =>
                  setFormState((current) =>
                    current ? { ...current, name: event.target.value } : current,
                  )
                }
                className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
                maxLength={128}
                placeholder="例如：产品知识库"
              />
            </label>

            <label className="block">
              <span className="text-sm font-medium text-slate-700">描述</span>
              <input
                value={formState.description}
                onChange={(event) =>
                  setFormState((current) =>
                    current ? { ...current, description: event.target.value } : current,
                  )
                }
                className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
                maxLength={500}
                placeholder="用于管理产品文档和问答资料"
              />
            </label>
          </div>

          {formErrorMessage ? (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {formErrorMessage}
            </div>
          ) : null}

          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={isSaving}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              {isSaving ? '保存中...' : '保存'}
            </button>
            <button
              type="button"
              onClick={closeForm}
              disabled={isSaving}
              className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
            >
              取消
            </button>
          </div>
        </form>
      ) : null}

      <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="text-base font-semibold">知识库列表</h2>
          <span className="text-sm text-slate-500">共 {total} 个</span>
        </div>

        {isLoading ? (
          <div className="px-5 py-12 text-center text-sm text-slate-500">加载中...</div>
        ) : knowledgeBases.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-slate-700">暂无知识库，请先创建</p>
            <button
              type="button"
              onClick={openCreateForm}
              className="mt-4 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700"
            >
              新建知识库
            </button>
          </div>
        ) : (
          <ul className="divide-y divide-slate-200">
            {knowledgeBases.map((knowledgeBase) => (
              <li
                key={knowledgeBase.id}
                className="flex flex-col gap-4 px-5 py-4 md:flex-row md:items-center md:justify-between"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      onClick={() => navigate(`/admin/knowledge-bases/${knowledgeBase.id}`)}
                      className="truncate text-left text-base font-semibold text-slate-900 hover:text-slate-600"
                    >
                      {knowledgeBase.name}
                    </button>
                    {knowledgeBase.status ? (
                      <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                        状态 {knowledgeBase.status}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 text-sm text-slate-600">
                    {knowledgeBase.description || '暂无描述'}
                  </p>
                  <p className="mt-2 text-xs text-slate-500">
                    更新时间：{formatDate(knowledgeBase.updatedAt ?? knowledgeBase.createdAt)}
                  </p>
                </div>

                <div className="flex shrink-0 flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={() => navigate(`/chat`)}
                    className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-700"
                  >
                    去问答
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate(`/admin/knowledge-bases/${knowledgeBase.id}`)}
                    className="rounded border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                  >
                    进入详情
                  </button>
                  <button
                    type="button"
                    onClick={() => openEditForm(knowledgeBase)}
                    className="rounded border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                  >
                    编辑
                  </button>
                  <button
                    type="button"
                    onClick={() => openDeleteConfirm(knowledgeBase)}
                    disabled={deletingId !== null}
                    className="rounded border border-red-200 px-3 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:text-red-300"
                  >
                    {deletingId === knowledgeBase.id ? '删除中...' : '删除'}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {editOpen && editingKnowledgeBase ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6"
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-knowledge-base-title"
        >
          <form
            onSubmit={handleSaveEdit}
            className="w-full max-w-xl space-y-5 rounded-md bg-white p-6 shadow-xl"
          >
            <div>
              <h2 id="edit-knowledge-base-title" className="text-lg font-semibold text-slate-950">
                编辑知识库
              </h2>
              <p className="mt-1 text-sm text-slate-500">名称必填，描述可选。</p>
            </div>

            <div className="space-y-4">
              <label className="block">
                <span className="text-sm font-medium text-slate-700">名称</span>
                <input
                  value={editForm.name}
                  onChange={(event) =>
                    setEditForm((current) => ({ ...current, name: event.target.value }))
                  }
                  className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
                  maxLength={128}
                  placeholder="例如：产品知识库"
                  autoFocus
                />
              </label>

              <label className="block">
                <span className="text-sm font-medium text-slate-700">描述</span>
                <input
                  value={editForm.description}
                  onChange={(event) =>
                    setEditForm((current) => ({ ...current, description: event.target.value }))
                  }
                  className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
                  maxLength={500}
                  placeholder="用于管理产品文档和问答资料"
                />
              </label>
            </div>

            {editError ? (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {editError}
              </div>
            ) : null}

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={closeEditDialog}
                disabled={savingEdit}
                className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
              >
                取消
              </button>
              <button
                type="submit"
                disabled={savingEdit}
                className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
              >
                {savingEdit ? '保存中...' : '保存'}
              </button>
            </div>
          </form>
        </div>
      ) : null}

      {pendingDeleteKnowledgeBase ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-knowledge-base-title"
        >
          <div className="w-full max-w-md rounded-md bg-white p-6 shadow-xl">
            <h2 id="delete-knowledge-base-title" className="text-lg font-semibold text-slate-950">
              确认删除知识库？
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              你确定要删除知识库「
              <span className="font-semibold text-slate-950">{pendingDeleteKnowledgeBase.name}</span>
              」吗？删除后该知识库将不可用，相关文档、解析结果、向量数据、RAG
              问答能力和历史会话 / 问答记录可能无法恢复。
            </p>

            {deleteErrorMessage ? (
              <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {deleteErrorMessage}
              </div>
            ) : null}

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={closeDeleteConfirm}
                disabled={deletingId !== null}
                autoFocus
                className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void confirmDeleteKnowledgeBase()}
                disabled={deletingId !== null}
                className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-red-300"
              >
                {deletingId === pendingDeleteKnowledgeBase.id ? '删除中...' : '确认删除'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
