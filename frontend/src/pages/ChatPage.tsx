import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'

import { askChatQuestion } from '../api/chat'
import { getConversationDetail, getConversations } from '../api/conversation'
import { getDocumentsByKnowledgeBaseId } from '../api/document'
import { getKnowledgeBaseById, getKnowledgeBases } from '../api/knowledgeBase'
import { ChatInput } from '../components/chat/ChatInput'
import { ChatMessageList } from '../components/chat/ChatMessageList'
import { ConversationSidebar } from '../components/chat/ConversationSidebar'
import type { ChatAskResponse, ChatMessage } from '../types/chat'
import type { ConversationDetail, ConversationMessage, ConversationSummary } from '../types/conversation'
import type { KnowledgeDocument } from '../types/document'
import type { KnowledgeBase } from '../types/knowledgeBase'
import { normalizeSources } from '../utils/chatSources'

const DEFAULT_PAGE_NUM = 1
const DEFAULT_PAGE_SIZE = 50
const CONVERSATION_QUERY_KEY = 'conversationId'

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
      return '登录已失效，请重新登录后再操作。'
    }

    if (error.response?.status === 403) {
      return '当前账号没有访问该会话或知识库的权限。'
    }

    if (error.response?.status === 404) {
      return '会话不存在、已删除，或不属于当前知识库。'
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

function normalizeAnswer(answer?: string | null) {
  const trimmed = answer?.trim() ?? ''

  if (!trimmed) {
    return '后端未返回可展示的回答。'
  }

  try {
    const parsed = JSON.parse(trimmed) as unknown
    const texts = readTextFromUnknown(parsed)

    // Some LLM vendors return the answer as a JSON string; extract readable text when possible.
    if (texts.length > 0) {
      return texts.join('\n\n')
    }
  } catch {
    return trimmed
  }

  return trimmed
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

function truncateTitle(question: string) {
  const trimmed = question.trim()
  return trimmed.length > 30 ? `${trimmed.slice(0, 30)}...` : trimmed
}

function mapRole(role: ConversationMessage['role']): ChatMessage['role'] {
  return role === 'USER' || role === 'user' ? 'user' : 'assistant'
}

function mapConversationMessages(detail: ConversationDetail): ChatMessage[] {
  const indexedMessages = (detail.messages ?? []).map((message, index) => ({
    message,
    index,
  }))

  const sortedMessages = indexedMessages.every(({ message }) => message.createdAt)
    ? [...indexedMessages].sort(
        (first, second) =>
          new Date(first.message.createdAt ?? '').getTime() -
          new Date(second.message.createdAt ?? '').getTime(),
      )
    : indexedMessages

  // Historical messages are normalized at the edge so the UI always receives stable ids,
  // lowercase roles, safe content, and parsed citation arrays.
  return sortedMessages.map(({ message, index }) => {
    const role = mapRole(message.role)
    const content = message.content?.trim() || (role === 'user' ? '用户消息为空' : 'AI 回答为空')
    const fallbackId = `${detail.conversationId}-${message.createdAt ?? 'no-time'}-${index}`

    return {
      id: message.messageId || fallbackId,
      role,
      content: role === 'assistant' ? normalizeAnswer(content) : content,
      citations: role === 'assistant' ? normalizeSources(message.citations) : [],
      createdAt: message.createdAt,
      status: 'success' as const,
    }
  })
}

function conversationRecords(data?: { list?: ConversationSummary[]; records?: ConversationSummary[] } | null) {
  return data?.records ?? data?.list ?? []
}

export function ChatPage() {
  const navigate = useNavigate()
  const { knowledgeBaseId: routeKnowledgeBaseId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const listBottomRef = useRef<HTMLDivElement | null>(null)
  const detailRequestSeqRef = useRef(0)
  const sendRequestSeqRef = useRef(0)
  const latestKnowledgeBaseIdRef = useRef<number | null>(null)
  const latestConversationIdRef = useRef<string | undefined>(undefined)

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState(routeKnowledgeBaseId ?? '')
  const [currentKnowledgeBase, setCurrentKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [isInitialLoading, setIsInitialLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [isConversationListLoading, setIsConversationListLoading] = useState(false)
  const [isConversationDetailLoading, setIsConversationDetailLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [conversationListError, setConversationListError] = useState('')
  const [question, setQuestion] = useState('')
  const [conversationId, setConversationId] = useState<string | undefined>()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [conversations, setConversations] = useState<ConversationSummary[]>([])

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
    latestKnowledgeBaseIdRef.current = selectedIdNumber
  }, [selectedIdNumber])

  useEffect(() => {
    latestConversationIdRef.current = conversationId
  }, [conversationId])

  const replaceConversationQuery = useCallback(
    (nextConversationId?: string) => {
      const nextParams = new URLSearchParams(searchParams)

      if (nextConversationId) {
        nextParams.set(CONVERSATION_QUERY_KEY, nextConversationId)
      } else {
        nextParams.delete(CONVERSATION_QUERY_KEY)
      }

      setSearchParams(nextParams, { replace: true })
    },
    [searchParams, setSearchParams],
  )

  const loadConversations = useCallback(
    async (knowledgeBaseId: number) => {
      setIsConversationListLoading(true)
      setConversationListError('')

      try {
        const response = await getConversations({
          knowledgeBaseId,
          page: DEFAULT_PAGE_NUM,
          size: DEFAULT_PAGE_SIZE,
        })

        if (response.code !== 0 || !response.data) {
          setConversations([])
          setConversationListError(response.message || '历史会话加载失败。')
          return
        }

        const nextConversations = conversationRecords(response.data).filter(
          (conversation) => String(conversation.knowledgeBaseId) === String(knowledgeBaseId),
        )
        setConversations(nextConversations)
      } catch (error) {
        setConversations([])
        setConversationListError(getErrorMessage(error, '历史会话加载失败，请稍后重试。'))
      } finally {
        setIsConversationListLoading(false)
      }
    },
    [],
  )

  const loadConversationDetail = useCallback(
    async (nextConversationId: string, options?: { fromUrl?: boolean }) => {
      if (selectedIdNumber === null) {
        return
      }

      const requestSeq = detailRequestSeqRef.current + 1
      detailRequestSeqRef.current = requestSeq
      setIsConversationDetailLoading(true)
      setErrorMessage('')

      try {
        const response = await getConversationDetail(nextConversationId)

        if (detailRequestSeqRef.current !== requestSeq) {
          return
        }

        if (response.code !== 0 || !response.data) {
          throw new Error(response.message || '会话详情加载失败。')
        }

        if (String(response.data.knowledgeBaseId) !== String(selectedIdNumber)) {
          throw new Error('该会话不属于当前知识库。')
        }

        setConversationId(response.data.conversationId)
        setMessages(mapConversationMessages(response.data))
        replaceConversationQuery(response.data.conversationId)
      } catch (error) {
        if (detailRequestSeqRef.current !== requestSeq) {
          return
        }

        setErrorMessage(getErrorMessage(error, '会话详情加载失败，请稍后重试。'))
        if (options?.fromUrl) {
          setConversationId(undefined)
          setMessages([])
          replaceConversationQuery(undefined)
        }
      } finally {
        if (detailRequestSeqRef.current === requestSeq) {
          setIsConversationDetailLoading(false)
        }
      }
    },
    [replaceConversationQuery, selectedIdNumber],
  )

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
          setErrorMessage(response.message || '知识库列表加载失败。')
          setKnowledgeBases([])
          return
        }

        const list = response.data.list ?? []
        setKnowledgeBases(list)

        if (!selectedKnowledgeBaseId && list[0]) {
          setSelectedKnowledgeBaseId(String(list[0].id))
        }
      } catch (error) {
        setErrorMessage(getErrorMessage(error, '知识库列表加载失败，请稍后重试。'))
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
        setConversations([])
        setConversationId(undefined)
        setMessages([])
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
          setErrorMessage(knowledgeBaseResponse.message || '知识库信息加载失败。')
          return
        }

        setCurrentKnowledgeBase(knowledgeBaseResponse.data)
        setDocuments(documentsResponse.code === 0 && documentsResponse.data ? documentsResponse.data : [])
      } catch (error) {
        setCurrentKnowledgeBase(null)
        setDocuments([])
        setErrorMessage(getErrorMessage(error, '知识库信息加载失败，请稍后重试。'))
      }
    }

    void loadCurrentKnowledgeBase()
  }, [selectedIdNumber])

  useEffect(() => {
    if (selectedIdNumber === null) {
      return
    }

    setConversationId(undefined)
    setMessages([])
    setQuestion('')
    void loadConversations(selectedIdNumber)
  }, [loadConversations, selectedIdNumber])

  useEffect(() => {
    if (selectedIdNumber === null) {
      return
    }

    const queryConversationId = searchParams.get(CONVERSATION_QUERY_KEY)?.trim()
    if (!queryConversationId || queryConversationId === conversationId) {
      return
    }

    // URL restoration is intentionally best-effort; backend authorization remains authoritative.
    void loadConversationDetail(queryConversationId, { fromUrl: true })
  }, [conversationId, loadConversationDetail, searchParams, selectedIdNumber])

  useEffect(() => {
    listBottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, isSending, isConversationDetailLoading])

  function handleKnowledgeBaseChange(value: string) {
    setSelectedKnowledgeBaseId(value)
    setConversationId(undefined)
    setMessages([])
    setQuestion('')
    setErrorMessage('')
    setConversationListError('')

    if (value) {
      navigate(hasRouteKnowledgeBase ? `/knowledge-bases/${value}/chat` : `/chat`, { replace: true })
      setSearchParams({}, { replace: true })
    }
  }

  function handleNewConversation() {
    detailRequestSeqRef.current += 1
    setConversationId(undefined)
    setMessages([])
    setQuestion('')
    setErrorMessage('')
    setIsConversationDetailLoading(false)
    replaceConversationQuery(undefined)
  }

  function handleSelectConversation(nextConversationId: string) {
    if (nextConversationId === conversationId) {
      return
    }

    setConversationId(nextConversationId)
    setQuestion('')
    replaceConversationQuery(nextConversationId)
    void loadConversationDetail(nextConversationId)
  }

  function updateConversationFromAnswer(answer: ChatAskResponse, sentQuestion: string) {
    const nextConversationId = answer.conversationId?.trim()
    if (!nextConversationId || selectedIdNumber === null) {
      return
    }

    setConversations((current) => {
      const existing = current.find((conversation) => conversation.conversationId === nextConversationId)
      const updated: ConversationSummary = {
        conversationId: nextConversationId,
        knowledgeBaseId: selectedIdNumber,
        title: existing?.title || truncateTitle(sentQuestion),
        messageCount: (existing?.messageCount ?? 0) + 2,
        lastQuestion: sentQuestion,
        lastAnswerPreview: normalizeAnswer(answer.answer).slice(0, 120),
        lastActiveAt: new Date().toISOString(),
        createdAt: existing?.createdAt,
      }

      return [updated, ...current.filter((conversation) => conversation.conversationId !== nextConversationId)]
    })
  }

  async function handleSend() {
    const trimmedQuestion = question.trim()

    if (!trimmedQuestion || isSending || selectedIdNumber === null) {
      return
    }

    const requestKnowledgeBaseId = selectedIdNumber
    const requestConversationId = conversationId
    const userMessageId = createMessageId('user')
    const requestSeq = sendRequestSeqRef.current + 1
    sendRequestSeqRef.current = requestSeq

    setMessages((current) => [
      ...current,
      {
        id: userMessageId,
        role: 'user',
        content: trimmedQuestion,
        status: 'sending',
      },
    ])
    setQuestion('')
    setIsSending(true)
    setErrorMessage('')

    try {
      const response = await askChatQuestion({
        knowledgeBaseId: requestKnowledgeBaseId,
        question: trimmedQuestion,
        // Existing sessions must reuse conversationId; new sessions omit it and let the backend create one.
        conversationId: requestConversationId,
      })

      if (response.code !== 0 || !response.data) {
        throw new Error(response.message || '问答请求失败，请稍后重试。')
      }

      const chatResponse = response.data
      const nextConversationId = chatResponse.conversationId?.trim()
      const stillOnSameTarget =
        latestKnowledgeBaseIdRef.current === requestKnowledgeBaseId &&
        latestConversationIdRef.current === requestConversationId

      updateConversationFromAnswer(chatResponse, trimmedQuestion)

      if (nextConversationId && stillOnSameTarget) {
        setConversationId(nextConversationId)
        replaceConversationQuery(nextConversationId)
      }

      if (!stillOnSameTarget || sendRequestSeqRef.current !== requestSeq) {
        return
      }

      setMessages((current) => [
        ...current.map((message) =>
          message.id === userMessageId ? { ...message, status: 'success' as const } : message,
        ),
        {
          id: createMessageId('assistant'),
          role: 'assistant',
          content: normalizeAnswer(chatResponse.answer),
          citations: normalizeSources(chatResponse),
          response: chatResponse,
          status: 'success',
        },
      ])

      void loadConversations(requestKnowledgeBaseId)
    } catch (error) {
      const stillOnSameTarget =
        latestKnowledgeBaseIdRef.current === requestKnowledgeBaseId &&
        latestConversationIdRef.current === requestConversationId

      if (stillOnSameTarget) {
        setMessages((current) =>
          current.map((message) =>
            message.id === userMessageId ? { ...message, status: 'failed' as const } : message,
          ),
        )
        setErrorMessage(getErrorMessage(error, '问答请求失败，请稍后重试。'))
      }
    } finally {
      if (sendRequestSeqRef.current === requestSeq) {
        setIsSending(false)
      }
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
              <span className="rounded bg-slate-100 px-2 py-1 text-xs text-slate-500">
                已恢复会话上下文
              </span>
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

      <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-[20rem_minmax(0,1fr)]">
        <ConversationSidebar
          conversations={conversations}
          currentConversationId={conversationId}
          loading={isConversationListLoading}
          errorMessage={conversationListError}
          onSelect={handleSelectConversation}
          onNewConversation={handleNewConversation}
        />

        <div className="flex min-h-[32rem] flex-col overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <div className="flex-1 overflow-y-auto px-5 py-5">
            <ChatMessageList
              messages={messages}
              sending={isSending}
              detailLoading={isConversationDetailLoading}
            />
            <div ref={listBottomRef} />
          </div>

          <div className="border-t border-slate-200 p-4">
            <ChatInput
              value={question}
              onChange={setQuestion}
              onSend={() => void handleSend()}
              sending={isSending}
              disabled={isSending || selectedIdNumber === null || isConversationDetailLoading}
            />
          </div>
        </div>
      </div>
    </section>
  )
}
