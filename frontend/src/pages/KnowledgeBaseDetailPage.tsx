import type { ChangeEvent } from 'react'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
  embedDocument,
  getDocumentsByKnowledgeBaseId,
  processDocument,
  uploadDocument,
} from '../api/document'
import { getKnowledgeBaseById } from '../api/knowledgeBase'
import type { DocumentEmbeddingResponse, KnowledgeDocument } from '../types/document'
import type { KnowledgeBase } from '../types/knowledgeBase'

const SUPPORTED_EXTENSIONS = ['txt', 'md', 'pdf', 'docx'] as const
const EMBEDDING_POLL_INTERVAL_MS = 2000
const EMBEDDING_POLL_TIMEOUT_MS = 60000

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
    if (error.response?.status === 401) {
      return '登录已失效或尚未登录，请重新登录后再操作'
    }

    return error.response?.data?.message || fallback
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

function formatFileSize(size?: number | null) {
  if (size === null || size === undefined) {
    return '-'
  }

  if (size < 1024) {
    return `${size} B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function getFileExtension(fileName: string) {
  const extension = fileName.split('.').pop()?.toLowerCase()
  return extension ?? ''
}

function isSupportedFile(file: File) {
  return SUPPORTED_EXTENSIONS.includes(getFileExtension(file.name) as (typeof SUPPORTED_EXTENSIONS)[number])
}

function statusLabel(status?: string | null) {
  if (!status) {
    return '-'
  }

  const labels: Record<string, string> = {
    NOT_STARTED: '未开始',
    PENDING: '排队中',
    PROCESSING: '处理中',
    RUNNING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
  }

  return labels[status] ?? status
}

function isBusyStatus(status?: string | null) {
  return status === 'PROCESSING' || status === 'RUNNING' || status === 'PENDING'
}

function isParsed(document: KnowledgeDocument) {
  return document.parseStatus === 'SUCCESS'
}

function isEmbedded(document: KnowledgeDocument) {
  return document.embeddingStatus === 'SUCCESS'
}

function shouldShowDocumentError(document: KnowledgeDocument) {
  return Boolean(
    document.latestErrorMessage &&
      (document.latestTaskStatus === 'FAILED' ||
        document.parseStatus === 'FAILED' ||
        document.embeddingStatus === 'FAILED'),
  )
}

function getEmbeddingSuccessMessage(result?: DocumentEmbeddingResponse | null) {
  const total = result?.total ?? 0
  const successCount = result?.successCount ?? 0
  const failedCount = result?.failedCount ?? 0

  if (total === 0) {
    return '未找到可向量化的文档切片，请先确认文档已完成解析。'
  }

  if (failedCount > 0) {
    return `Embedding 部分完成，成功 ${successCount} 个，失败 ${failedCount} 个。`
  }

  if (successCount > 0) {
    return `Embedding 完成，成功处理 ${successCount} 个切片。`
  }

  if (result?.taskStatus === 'SUCCESS') {
    return 'Embedding 任务执行成功。'
  }

  if (isBusyStatus(result?.taskStatus)) {
    return '向量生成中。'
  }

  return 'Embedding 任务已提交。'
}

function resolveDocumentId(document: KnowledgeDocument) {
  return document.documentId ?? document.id
}

export function KnowledgeBaseDetailPage() {
  const { id } = useParams()
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const knowledgeBaseId = Number(id)
  const hasValidId = Number.isInteger(knowledgeBaseId) && knowledgeBaseId > 0

  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isDocumentLoading, setIsDocumentLoading] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [processingId, setProcessingId] = useState<number | null>(null)
  const [embeddingId, setEmbeddingId] = useState<number | null>(null)
  const [embeddingProcessingIds, setEmbeddingProcessingIds] = useState<Set<number>>(() => new Set())
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [uploadErrorMessage, setUploadErrorMessage] = useState('')
  const embeddingPollTimersRef = useRef<Map<number, number>>(new Map())
  const embeddingPollStartedAtRef = useRef<Map<number, number>>(new Map())
  const embeddingObservedRunningRef = useRef<Set<number>>(new Set())
  const canUseKnowledgeBase = hasValidId && knowledgeBase !== null

  async function loadDetail() {
    if (!hasValidId) {
      setErrorMessage('知识库 ID 无效')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setErrorMessage('')

    try {
      const [knowledgeBaseResponse, documentsResponse] = await Promise.all([
        getKnowledgeBaseById(knowledgeBaseId),
        getDocumentsByKnowledgeBaseId(knowledgeBaseId),
      ])

      if (knowledgeBaseResponse.code !== 0 || !knowledgeBaseResponse.data) {
        setKnowledgeBase(null)
        setDocuments([])
        setErrorMessage(knowledgeBaseResponse.message || '知识库详情加载失败')
        return
      }

      if (documentsResponse.code !== 0 || !documentsResponse.data) {
        setKnowledgeBase(knowledgeBaseResponse.data)
        setDocuments([])
        setErrorMessage(documentsResponse.message || '文档列表加载失败')
        return
      }

      setKnowledgeBase(knowledgeBaseResponse.data)
      setDocuments(documentsResponse.data)
    } catch (error) {
      setKnowledgeBase(null)
      setDocuments([])
      setErrorMessage(getErrorMessage(error, '知识库详情加载失败，请稍后重试'))
    } finally {
      setIsLoading(false)
    }
  }

  async function loadDocuments(options: { silent?: boolean; clearError?: boolean } = {}) {
    if (!hasValidId) {
      return null
    }

    if (!options.silent) {
      setIsDocumentLoading(true)
    }

    try {
      const response = await getDocumentsByKnowledgeBaseId(knowledgeBaseId)

      if (response.code !== 0 || !response.data) {
        setErrorMessage(response.message || '文档列表刷新失败')
        setDocuments([])
        return null
      }

      if (options.clearError !== false) {
        setErrorMessage('')
      }
      setDocuments(response.data)
      return response.data
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '文档列表刷新失败，请稍后重试'))
      setDocuments([])
      return null
    } finally {
      if (!options.silent) {
        setIsDocumentLoading(false)
      }
    }
  }

  useEffect(() => {
    void loadDetail()
  }, [hasValidId, knowledgeBaseId])

  useEffect(() => {
    return () => {
      embeddingPollTimersRef.current.forEach((timerId) => window.clearTimeout(timerId))
      embeddingPollTimersRef.current.clear()
      embeddingPollStartedAtRef.current.clear()
      embeddingObservedRunningRef.current.clear()
    }
  }, [])

  function setDocumentEmbeddingProcessing(documentId: number, isProcessing: boolean) {
    setEmbeddingProcessingIds((current) => {
      const next = new Set(current)

      if (isProcessing) {
        next.add(documentId)
      } else {
        next.delete(documentId)
      }

      return next
    })
  }

  function stopEmbeddingPolling(documentId: number) {
    const timerId = embeddingPollTimersRef.current.get(documentId)

    if (timerId !== undefined) {
      window.clearTimeout(timerId)
      embeddingPollTimersRef.current.delete(documentId)
    }

    embeddingPollStartedAtRef.current.delete(documentId)
    embeddingObservedRunningRef.current.delete(documentId)
    setDocumentEmbeddingProcessing(documentId, false)
  }

  function startEmbeddingPolling(documentId: number) {
    stopEmbeddingPolling(documentId)
    setDocumentEmbeddingProcessing(documentId, true)
    embeddingPollStartedAtRef.current.set(documentId, Date.now())

    const pollOnce = async () => {
      const latestDocuments = await loadDocuments({ silent: true, clearError: false })
      const targetDocument = latestDocuments?.find((item) => resolveDocumentId(item) === documentId)

      if (!targetDocument) {
        scheduleNextPoll()
        return
      }

      const elapsed = Date.now() - (embeddingPollStartedAtRef.current.get(documentId) ?? Date.now())
      const taskStillRunning =
        isBusyStatus(targetDocument.embeddingStatus) || isBusyStatus(targetDocument.latestTaskStatus)

      if (taskStillRunning) {
        embeddingObservedRunningRef.current.add(documentId)
        scheduleNextPoll()
        return
      }

      if (targetDocument.embeddingStatus === 'SUCCESS' || targetDocument.latestTaskStatus === 'SUCCESS') {
        stopEmbeddingPolling(documentId)
        setSuccessMessage(
          targetDocument.embeddedChunkCount && targetDocument.embeddedChunkCount > 0
            ? `向量化完成，成功处理 ${targetDocument.embeddedChunkCount} 个切片。`
            : 'Embedding 任务执行成功。',
        )
        return
      }

      if (targetDocument.latestTaskStatus === 'FAILED' || targetDocument.embeddingStatus === 'FAILED') {
        stopEmbeddingPolling(documentId)
        setErrorMessage(targetDocument.latestErrorMessage || 'Embedding 任务失败')
        return
      }

      if (elapsed >= EMBEDDING_POLL_TIMEOUT_MS) {
        stopEmbeddingPolling(documentId)
        setSuccessMessage('向量生成仍在处理中，请稍后刷新查看结果。')
        return
      }

      scheduleNextPoll()
    }

    const scheduleNextPoll = () => {
      if (!embeddingPollStartedAtRef.current.has(documentId)) {
        return
      }

      const timerId = window.setTimeout(() => {
        void pollOnce()
      }, EMBEDDING_POLL_INTERVAL_MS)

      embeddingPollTimersRef.current.set(documentId, timerId)
    }

    const timerId = window.setTimeout(() => {
      void pollOnce()
    }, EMBEDDING_POLL_INTERVAL_MS)
    embeddingPollTimersRef.current.set(documentId, timerId)
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setSuccessMessage('')
    setUploadErrorMessage('')

    if (!file) {
      setSelectedFile(null)
      return
    }

    // 文件类型校验：当前后端只支持 txt、md、pdf、docx 文件。
    if (!isSupportedFile(file)) {
      setSelectedFile(null)
      setUploadErrorMessage('仅支持上传 .txt、.md、.pdf、.docx 文件')
      event.target.value = ''
      return
    }

    setSelectedFile(file)
  }

  async function handleUpload() {
    if (!hasValidId || isUploading) {
      return
    }

    if (!knowledgeBase) {
      setUploadErrorMessage('知识库不存在或不可用，不能上传文档')
      return
    }

    if (!selectedFile) {
      setUploadErrorMessage('请先选择 .txt、.md、.pdf 或 .docx 文件')
      return
    }

    // 上传逻辑：携带当前 knowledgeBaseId，并在上传期间禁用按钮防止重复提交。
    if (!isSupportedFile(selectedFile)) {
      setUploadErrorMessage('仅支持上传 .txt、.md、.pdf、.docx 文件')
      return
    }

    setIsUploading(true)
    setErrorMessage('')
    setSuccessMessage('')
    setUploadErrorMessage('')

    try {
      const response = await uploadDocument(knowledgeBaseId, selectedFile)

      if (response.code !== 0 || !response.data) {
        setUploadErrorMessage(response.message || '文档上传失败')
        return
      }

      setSelectedFile(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      setSuccessMessage('文档上传成功')
      await loadDocuments()
    } catch (error) {
      setUploadErrorMessage(getErrorMessage(error, '文档上传失败，请稍后重试'))
    } finally {
      setIsUploading(false)
    }
  }

  async function handleProcess(document: KnowledgeDocument) {
    const documentId = resolveDocumentId(document)

    if (processingId !== null || embeddingId !== null || embeddingProcessingIds.size > 0) {
      return
    }

    // process 调用逻辑：真实文件已上传到后端，这里不传 textContent，交由后端读取 txt/md 文件并切片。
    setProcessingId(documentId)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await processDocument(documentId)

      if (response.code !== 0) {
        setErrorMessage(response.message || '文档解析任务提交失败')
        return
      }

      setSuccessMessage('文档解析任务已提交')
      await loadDocuments()
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '文档解析任务提交失败，请稍后重试'))
    } finally {
      setProcessingId(null)
    }
  }

  async function handleEmbed(document: KnowledgeDocument) {
    const documentId = resolveDocumentId(document)

    if (processingId !== null || embeddingId !== null || embeddingProcessingIds.has(documentId)) {
      return
    }

    if (!isParsed(document)) {
      setErrorMessage('请先解析文档')
      return
    }

    if ((document.chunkCount ?? 0) <= 0) {
      setErrorMessage('未找到可向量化的切片，请先解析文档')
      return
    }

    // embedding 调用逻辑：只提交文档 ID，不在前端暴露模型、API Key 等敏感配置。
    setEmbeddingId(documentId)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await embedDocument(documentId)

      if (response.code !== 0) {
        setErrorMessage(response.message || 'Embedding 失败')
        return
      }

      setSuccessMessage(getEmbeddingSuccessMessage(response.data))
      if (isBusyStatus(response.data?.taskStatus)) {
        startEmbeddingPolling(documentId)
      }
      await loadDocuments({ clearError: false })
    } catch (error) {
      setErrorMessage(getErrorMessage(error, 'Embedding 请求失败，请检查网络或后端服务。'))
    } finally {
      setEmbeddingId(null)
    }
  }

  if (isLoading) {
    return <div className="px-1 py-12 text-center text-sm text-slate-500">加载中...</div>
  }

  return (
    <section className="space-y-6">
      <header className="rounded-lg border border-slate-200 bg-white px-5 py-5 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0 flex-1">
            <Link
              to="/knowledge-bases"
              className="inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100 hover:text-slate-950 active:bg-slate-200 focus:outline-none focus:ring-2 focus:ring-slate-300 focus:ring-offset-2"
            >
              ← 返回知识库列表
            </Link>
            <h1 className="mt-4 break-words text-2xl font-semibold text-slate-950">
              {knowledgeBase?.name ?? '知识库详情'}
            </h1>
            <p className="mt-2 max-w-3xl break-words text-sm leading-6 text-slate-600">
              {knowledgeBase?.description || '暂无描述'}
            </p>
            {knowledgeBase?.updatedAt || knowledgeBase?.createdAt ? (
              <p className="mt-2 text-xs text-slate-500">
                更新时间：{formatDate(knowledgeBase.updatedAt ?? knowledgeBase.createdAt)}
              </p>
            ) : null}
          </div>
          <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center lg:justify-end">
            {canUseKnowledgeBase ? (
              <Link
                to={`/knowledge-bases/${knowledgeBaseId}/chat`}
                aria-label="进入知识库问答"
                className="inline-flex min-h-12 w-full items-center justify-center rounded-md bg-slate-950 px-5 py-3 text-base font-semibold text-white shadow-sm transition hover:bg-slate-800 active:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 sm:w-auto"
              >
                进入知识库问答
              </Link>
            ) : null}
            <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              文档数：<span className="font-semibold text-slate-900">{documents.length}</span>
            </div>
          </div>
        </div>
      </header>

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

      <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-base font-semibold text-slate-950">上传文档</h2>
            <p className="mt-1 text-sm text-slate-500">支持 .txt、.md、.pdf、.docx 文件。</p>
          </div>
          <div className="flex w-full flex-col gap-3 sm:flex-row lg:w-auto">
            <input
              ref={fileInputRef}
              type="file"
              accept=".txt,.md,.pdf,.docx"
              disabled={!canUseKnowledgeBase || isUploading}
              onChange={handleFileChange}
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm file:mr-3 file:rounded file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-slate-700 disabled:cursor-not-allowed disabled:bg-slate-100 sm:w-80"
            />
            <button
              type="button"
              onClick={() => void handleUpload()}
              disabled={!canUseKnowledgeBase || isUploading || !selectedFile}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              {isUploading ? '上传中...' : '上传'}
            </button>
          </div>
        </div>
        {selectedFile ? (
          <p className="mt-3 text-sm text-slate-600">
            已选择：{selectedFile.name}（{formatFileSize(selectedFile.size)}）
          </p>
        ) : null}
        {uploadErrorMessage ? (
          <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {uploadErrorMessage}
          </div>
        ) : null}
      </div>

      <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-950">文档列表</h2>
          <button
            type="button"
            onClick={() => void loadDocuments()}
            disabled={
              isDocumentLoading ||
              processingId !== null ||
              embeddingId !== null ||
              embeddingProcessingIds.size > 0 ||
              isUploading
            }
            className="rounded border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
          >
            {isDocumentLoading ? '刷新中...' : '刷新'}
          </button>
        </div>

        {isDocumentLoading ? (
          <div className="px-5 py-12 text-center text-sm text-slate-500">文档加载中...</div>
        ) : documents.length === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-slate-500">
            暂无文档，请先上传文件。
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3">文档名称</th>
                  <th className="px-5 py-3">类型</th>
                  <th className="px-5 py-3">大小</th>
                  <th className="px-5 py-3">解析状态</th>
                  <th className="px-5 py-3">Embedding 状态</th>
                  <th className="px-5 py-3">创建时间</th>
                  <th className="px-5 py-3">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {documents.map((document) => {
                  const documentId = resolveDocumentId(document)
                  const processBusy = processingId === documentId || isBusyStatus(document.parseStatus)
                  const localEmbeddingProcessing = embeddingProcessingIds.has(documentId)
                  const embedBusy =
                    embeddingId === documentId ||
                    localEmbeddingProcessing ||
                    isBusyStatus(document.embeddingStatus)
                  const embeddingStatus = localEmbeddingProcessing ? 'PROCESSING' : document.embeddingStatus
                  const canEmbed = isParsed(document) && (document.chunkCount ?? 0) > 0 && !localEmbeddingProcessing

                  return (
                    <tr key={documentId} className="align-top">
                      <td className="max-w-xs px-5 py-4">
                        <div className="break-words font-medium text-slate-900">{document.fileName}</div>
                        {localEmbeddingProcessing ? (
                          <div className="mt-1 break-words text-xs text-slate-500">
                            正在生成向量，请稍候
                          </div>
                        ) : shouldShowDocumentError(document) ? (
                          <div className="mt-1 break-words text-xs text-red-600">
                            {document.latestErrorMessage}
                          </div>
                        ) : null}
                      </td>
                      <td className="px-5 py-4 text-slate-600">{document.fileType || '-'}</td>
                      <td className="px-5 py-4 text-slate-600">{formatFileSize(document.fileSize)}</td>
                      <td className="px-5 py-4">
                        <span className="rounded bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700">
                          {statusLabel(document.parseStatus)}
                        </span>
                        <div className="mt-1 text-xs text-slate-500">
                          切片：{document.chunkCount ?? 0}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className="rounded bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700">
                          {localEmbeddingProcessing ? '生成中' : statusLabel(embeddingStatus)}
                        </span>
                        <div className="mt-1 text-xs text-slate-500">
                          已生成：{document.embeddedChunkCount ?? 0}
                        </div>
                      </td>
                      <td className="px-5 py-4 text-slate-600">{formatDate(document.createdAt)}</td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() => void handleProcess(document)}
                            disabled={
                              processBusy ||
                              processingId !== null ||
                              embeddingId !== null ||
                              embeddingProcessingIds.size > 0
                            }
                            className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
                          >
                            {processBusy ? '解析中...' : '解析'}
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleEmbed(document)}
                            disabled={!canEmbed || embedBusy || processingId !== null || embeddingId !== null}
                            title={
                              localEmbeddingProcessing
                                ? '正在生成向量，请稍候'
                                : !isParsed(document)
                                  ? '请先解析文档'
                                  : (document.chunkCount ?? 0) <= 0
                                    ? '未找到可向量化的切片，请先解析文档'
                                    : undefined
                            }
                            className="rounded border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
                          >
                            {embedBusy ? '生成中...' : isEmbedded(document) ? '重新生成' : '生成向量'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
