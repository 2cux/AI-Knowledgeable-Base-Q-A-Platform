import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
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

type FormMode = 'create' | 'edit'

type FormState = {
  mode: FormMode
  id?: number
  name: string
  description: string
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

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
  const [total, setTotal] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [formErrorMessage, setFormErrorMessage] = useState('')
  const [formState, setFormState] = useState<FormState | null>(null)

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
    setFormErrorMessage('')
    setFormState(createEmptyForm())
  }

  function openEditForm(knowledgeBase: KnowledgeBase) {
    setFormErrorMessage('')
    setFormState({
      mode: 'edit',
      id: knowledgeBase.id,
      name: knowledgeBase.name,
      description: knowledgeBase.description ?? '',
    })
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

    if (!formState) {
      return
    }

    const name = formState.name.trim()
    const description = formState.description.trim()

    if (!name) {
      setFormErrorMessage('请输入知识库名称')
      return
    }

    setIsSaving(true)
    setFormErrorMessage('')

    try {
      const response =
        formState.mode === 'create'
          ? await createKnowledgeBase({ name, description })
          : await updateKnowledgeBase(formState.id!, { name, description })

      if (response.code !== 0 || !response.data) {
        setFormErrorMessage(response.message || '保存知识库失败')
        return
      }

      setFormState(null)
      await loadKnowledgeBases()
    } catch (error) {
      setFormErrorMessage(getErrorMessage(error, '保存知识库失败，请稍后重试'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(knowledgeBase: KnowledgeBase) {
    const confirmed = window.confirm(`确认删除知识库“${knowledgeBase.name}”吗？`)

    if (!confirmed) {
      return
    }

    setDeletingId(knowledgeBase.id)
    setErrorMessage('')

    try {
      const response = await deleteKnowledgeBase(knowledgeBase.id)

      if (response.code !== 0) {
        setErrorMessage(response.message || '删除知识库失败')
        return
      }

      await loadKnowledgeBases()
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '删除知识库失败，请稍后重试'))
    } finally {
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

      {formState ? (
        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div>
            <h2 className="text-base font-semibold">
              {formState.mode === 'create' ? '新建知识库' : '编辑知识库'}
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
                    <h3 className="truncate text-base font-semibold text-slate-900">
                      {knowledgeBase.name}
                    </h3>
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
                    onClick={() => navigate(`/knowledge-bases/${knowledgeBase.id}`)}
                    className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-700"
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
                    onClick={() => void handleDelete(knowledgeBase)}
                    disabled={deletingId === knowledgeBase.id}
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
    </section>
  )
}
