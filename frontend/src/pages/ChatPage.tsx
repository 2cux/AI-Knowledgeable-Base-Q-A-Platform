import type { KeyboardEvent } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { askChatQuestion } from '../api/chat'
import { SourceList } from '../components/chat/SourceList'
import { getDocumentsByKnowledgeBaseId } from '../api/document'
import { getKnowledgeBaseById, getKnowledgeBases } from '../api/knowledgeBase'
import type { ChatAskResponse } from '../types/chat'
import type { KnowledgeDocument } from '../types/document'
import type { KnowledgeBase } from '../types/knowledgeBase'
import { normalizeSources } from '../utils/chatSources'

const DEFAULT_PAGE_NUM = 1
const DEFAULT_PAGE_SIZE = 50

type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  response?: ChatAskResponse
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
    if (error.response?.status === 401) {
      return '登录已失效，请重新登录后再提问'
    }

    if (error.response?.status === 403) {
      return '当前账号没有访问该知识库的权限'
    }

    return error.response?.data?.message || fallback
  }

  if (error instanceof Error) {
    return error.message || fallback
  }

  return fallback
}

function hasText(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

function readTextFromUnknown(value: unknown): string[] {
  if (hasText(value)) {
    return [value.trim()]
  }

  if (Array.isArray(value)) {
    return value.flatMap(readTextFromUnknown)
  }

  if (!value || typeof value !== 'object') {
    return []
  }

  const record = value as Record<string, unknown>
  const directText = ['text', 'content', 'answer', 'message', 'output_text'].flatMap((key) =>
    readTextFromUnknown(record[key]),
  )

  if (directText.length > 0) {
    return directText
  }

  return ['content', 'data', 'items', 'messages', 'choices'].flatMap((key) =>
    readTextFromUnknown(record[key]),
  )
}

function normalizeAnswer(answer: string) {
  const trimmed = answer.trim()

  if (!trimmed) {
    return '后端未返回可展示的回答。'
  }

  try {
    const parsed = JSON.parse(trimmed) as unknown
    const texts = readTextFromUnknown(parsed)

    // 兼容 LLM 供应商返回 JSON 字符串的情况，例如 content 数组或 text 字段。
    if (texts.length > 0) {
      return texts.join('\n\n')
    }
  } catch {
    return answer
  }

  return answer
}

function isEmbeddingReady(documents: KnowledgeDocument[]) {
  if (documents.length === 0) {
    return false
  }

  return documents.some((document) => document.embeddingStatus === 'SUCCESS')
}

function createMessageId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function ResponseMeta({ response }: { response?: ChatAskResponse }) {
  if (!response) {
    return null
  }

  const citations = normalizeSources(response)
  const effectiveCount = response.effectiveChunkCount ?? response.retrievedChunkCount
  const rawCount = response.rawRetrievedChunkCount
  const shouldWarn = response.matched === false || effectiveCount === 0 || rawCount === 0

  return (
    <div className="mt-3 space-y-3">
      {shouldWarn ? (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          未检索到明确相关内容，以下回答可能不可靠。
        </div>
      ) : null}

      {citations.length > 0 ? (
        <SourceList sources={citations} />
      ) : effectiveCount !== null && effectiveCount !== undefined ? (
        <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
          检索命中：{effectiveCount} 个有效切片
          {rawCount !== null && rawCount !== undefined ? `，原始召回 ${rawCount} 个切片` : ''}
        </div>
      ) : null}
    </div>
  )
}

export function ChatPage() {
  const navigate = useNavigate()
  const { knowledgeBaseId: routeKnowledgeBaseId } = useParams()
  const listBottomRef = useRef<HTMLDivElement | null>(null)
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState(routeKnowledgeBaseId ?? '')
  const [currentKnowledgeBase, setCurrentKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [isInitialLoading, setIsInitialLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [question, setQuestion] = useState('')
  const [conversationId, setConversationId] = useState<string | undefined>()
  const [messages, setMessages] = useState<ChatMessage[]>([])

  const selectedIdNumber = useMemo(() => {
    const parsed = Number(selectedKnowledgeBaseId)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null
  }, [selectedKnowledgeBaseId])

  const selectableKnowledgeBases = useMemo(() => {
    if (
      currentKnowledgeBase &&
      !knowledgeBases.some((knowledgeBase) => knowledgeBase.id === currentKnowledgeBase.id)
    ) {
      return [...knowledgeBases, currentKnowledgeBase]
    }

    return knowledgeBases
  }, [currentKnowledgeBase, knowledgeBases])

  const hasRouteKnowledgeBase = Boolean(routeKnowledgeBaseId)
  const embeddingReady = isEmbeddingReady(documents)
  const shouldShowEmbeddingHint = selectedIdNumber !== null && documents.length > 0 && !embeddingReady

  useEffect(() => {
    if (routeKnowledgeBaseId) {
      setSelectedKnowledgeBaseId(routeKnowledgeBaseId)
    }
  }, [routeKnowledgeBaseId])

  useEffect(() => {
    async function loadKnowledgeBaseOptions() {
      setIsInitialLoading(true)
      setErrorMessage('')

      try {
        const response = await getKnowledgeBases({
          pageNum: DEFAULT_PAGE_NUM,
          pageSize: DEFAULT_PAGE_SIZE,
        })

        if (response.code !== 0 || !response.data) {
          setErrorMessage(response.message || '知识库列表加载失败')
          setKnowledgeBases([])
          return
        }

        const list = response.data.list ?? []
        setKnowledgeBases(list)

        if (!selectedKnowledgeBaseId && list[0]) {
          setSelectedKnowledgeBaseId(String(list[0].id))
        }
      } catch (error) {
        setErrorMessage(getErrorMessage(error, '知识库列表加载失败，请稍后重试'))
      } finally {
        setIsInitialLoading(false)
      }
    }

    void loadKnowledgeBaseOptions()
  }, [])

  useEffect(() => {
    async function loadCurrentKnowledgeBase() {
      if (selectedIdNumber === null) {
        setCurrentKnowledgeBase(null)
        setDocuments([])
        return
      }

      setErrorMessage('')

      try {
        const [knowledgeBaseResponse, documentsResponse] = await Promise.all([
          getKnowledgeBaseById(selectedIdNumber),
          getDocumentsByKnowledgeBaseId(selectedIdNumber),
        ])

        if (knowledgeBaseResponse.code !== 0 || !knowledgeBaseResponse.data) {
          setCurrentKnowledgeBase(null)
          setDocuments([])
          setErrorMessage(knowledgeBaseResponse.message || '知识库信息加载失败')
          return
        }

        setCurrentKnowledgeBase(knowledgeBaseResponse.data)

        if (documentsResponse.code === 0 && documentsResponse.data) {
          setDocuments(documentsResponse.data)
        } else {
          setDocuments([])
        }
      } catch (error) {
        setCurrentKnowledgeBase(null)
        setDocuments([])
        setErrorMessage(getErrorMessage(error, '知识库信息加载失败，请稍后重试'))
      }
    }

    void loadCurrentKnowledgeBase()
  }, [selectedIdNumber])

  useEffect(() => {
    listBottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, isSending])

  function handleKnowledgeBaseChange(value: string) {
    setSelectedKnowledgeBaseId(value)
    setConversationId(undefined)
    setMessages([])
    setQuestion('')
    setErrorMessage('')

    if (value) {
      navigate(hasRouteKnowledgeBase ? `/knowledge-bases/${value}/chat` : `/chat`, { replace: true })
    }
  }

  async function handleSend() {
    const trimmedQuestion = question.trim()

    if (!trimmedQuestion || isSending || selectedIdNumber === null) {
      return
    }

    const userMessage: ChatMessage = {
      id: createMessageId('user'),
      role: 'user',
      content: trimmedQuestion,
    }

    setMessages((current) => [...current, userMessage])
    setQuestion('')
    setIsSending(true)
    setErrorMessage('')

    try {
      const response = await askChatQuestion({
        knowledgeBaseId: selectedIdNumber,
        question: trimmedQuestion,
        // conversationId 由服务端首次返回后维护，后续追问复用同一会话上下文。
        conversationId,
      })

      if (response.code !== 0 && !response.data) {
        setErrorMessage(response.message || '问答请求失败，请稍后重试')
        return
      }

      if (!response.data) {
        setErrorMessage(response.message || '问答请求失败，请稍后重试')
        return
      }

      const chatResponse = response.data
      if (response.code !== 0) {
        setErrorMessage(response.message || '问答请求失败，请稍后重试')
      }
      const answer =
        typeof chatResponse.answer === 'string'
          ? chatResponse.answer
          : response.message || '问答请求失败，请稍后重试'
      const nextConversationId = chatResponse.conversationId?.trim()
      if (nextConversationId) {
        setConversationId(nextConversationId)
      }

      setMessages((current) => [
        ...current,
        {
          id: createMessageId('assistant'),
          role: 'assistant',
          content: normalizeAnswer(answer),
          response: chatResponse,
        },
      ])
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '问答请求失败，请稍后重试'))
    } finally {
      setIsSending(false)
    }
  }

  function handleInputKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void handleSend()
    }
  }

  if (isInitialLoading) {
    return <div className="px-1 py-12 text-center text-sm text-slate-500">加载中...</div>
  }

  return (
    <section className="flex min-h-[calc(100vh-8rem)] flex-col gap-5">
      <div className="flex flex-col gap-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-start md:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="break-words text-2xl font-semibold text-slate-950">
              {currentKnowledgeBase?.name ?? 'RAG 问答'}
            </h1>
            {conversationId ? (
              <span className="rounded bg-slate-100 px-2 py-1 text-xs text-slate-500">上下文已开启</span>
            ) : null}
          </div>
          <p className="mt-2 max-w-3xl break-words text-sm leading-6 text-slate-600">
            {currentKnowledgeBase?.description || '选择一个知识库后即可开始提问。'}
          </p>
          {hasRouteKnowledgeBase ? (
            <Link
              to={`/knowledge-bases/${selectedKnowledgeBaseId}`}
              className="mt-3 inline-flex text-sm font-medium text-slate-500 hover:text-slate-900"
            >
              返回知识库详情
            </Link>
          ) : null}
        </div>

        <label className="w-full md:w-72">
          <span className="text-sm font-medium text-slate-700">知识库</span>
          <select
            value={selectedKnowledgeBaseId}
            onChange={(event) => handleKnowledgeBaseChange(event.target.value)}
            className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
          >
            <option value="">请选择知识库</option>
            {selectableKnowledgeBases.map((knowledgeBase) => (
              <option key={knowledgeBase.id} value={knowledgeBase.id}>
                {knowledgeBase.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {errorMessage ? (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      {shouldShowEmbeddingHint ? (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          当前知识库尚未发现已完成 Embedding 的文档，请先在知识库详情页完成文档解析和 Embedding。
        </div>
      ) : null}

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
        <div className="flex-1 overflow-y-auto px-5 py-5">
          {messages.length === 0 ? (
            <div className="flex min-h-72 items-center justify-center text-center text-sm text-slate-500">
              请输入问题开始对话。页面刷新后暂不恢复历史记录。
            </div>
          ) : (
            <div className="space-y-5">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={[
                    'flex',
                    message.role === 'user' ? 'justify-end' : 'justify-start',
                  ].join(' ')}
                >
                  <div
                    className={[
                      'max-w-[88%] rounded-md px-4 py-3 text-sm leading-6',
                      message.role === 'user'
                        ? 'bg-slate-900 text-white'
                        : 'border border-slate-200 bg-white text-slate-800',
                    ].join(' ')}
                  >
                    <div className="whitespace-pre-wrap break-words">{message.content}</div>
                    {message.role === 'assistant' ? <ResponseMeta response={message.response} /> : null}
                  </div>
                </div>
              ))}

              {isSending ? (
                <div className="flex justify-start">
                  <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
                    正在生成回答...
                  </div>
                </div>
              ) : null}
            </div>
          )}
          <div ref={listBottomRef} />
        </div>

        <div className="border-t border-slate-200 p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <textarea
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={handleInputKeyDown}
              disabled={isSending || selectedIdNumber === null}
              rows={3}
              maxLength={2000}
              placeholder="输入问题，Enter 发送，Shift + Enter 换行"
              className="min-h-24 flex-1 resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-sm leading-6 outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200 disabled:cursor-not-allowed disabled:bg-slate-100"
            />
            <button
              type="button"
              onClick={() => void handleSend()}
              disabled={isSending || selectedIdNumber === null || !question.trim()}
              className="h-10 rounded bg-slate-900 px-5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400 md:w-24"
            >
              {isSending ? '发送中' : '发送'}
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
