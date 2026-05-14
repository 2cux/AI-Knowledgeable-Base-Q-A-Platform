import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import axios from 'axios'

import { askChatQuestion, askGlobalChatQuestion } from '../api/chat'
import {
  deleteConversation,
  getConversationDetail,
  getConversations,
  pinConversation,
  renameConversation,
} from '../api/conversation'
import { getDocumentsByKnowledgeBaseId } from '../api/document'
import {
  cancelFeedback,
  splitStoredFeedbackComment,
  submitFeedback,
  toClientFeedbackType,
  updateFeedback,
} from '../api/feedback'
import { getKnowledgeBaseById, getKnowledgeBases } from '../api/knowledgeBase'
import { ChatInput } from '../components/chat/ChatInput'
import { ChatMessageList } from '../components/chat/ChatMessageList'
import { ConversationSidebar } from '../components/chat/ConversationSidebar'
import { UserMenu } from '../components/UserMenu'
import type { ChatAskResponse, ChatMessage } from '../types/chat'
import type { ConversationDetail, ConversationMessage, ConversationSummary } from '../types/conversation'
import type { KnowledgeDocument } from '../types/document'
import type { FeedbackCancelRequest, FeedbackState, FeedbackSubmitRequest } from '../types/feedback'
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
  request?: unknown
  message?: string
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

    if (error.response?.status === 405) {
      return '请求方法不支持，请检查接口路径。'
    }

    if (error.response?.status && error.response.status >= 500) {
      return '服务异常，请稍后重试。'
    }

    return error.response?.data?.message || fallback
  }

  if (isResponseError(error) && error.request) {
    return '无法连接服务器，请检查后端是否启动。'
  }

  if (error instanceof Error) {
    // axios timeout error
    if (error.message?.includes('timeout') || (axios.isAxiosError(error) && error.code === 'ECONNABORTED')) {
      return '回答生成超时，可能是模型响应较慢，请稍后重试或缩短问题后再试。'
    }
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

function mapFeedback(message: ConversationMessage): FeedbackState | undefined {
  const feedbackType = toClientFeedbackType(message.feedback?.feedbackType ?? message.feedbackType)
  const feedbackComment = message.feedback?.comment ?? message.feedbackComment
  const storedComment = splitStoredFeedbackComment(feedbackComment)
  const reason = message.feedback?.reason ?? message.feedbackReason ?? storedComment.reason

  if (!feedbackType && !message.feedback?.submitted) {
    return undefined
  }

  return {
    feedbackId: message.feedback?.feedbackId ?? message.feedbackId ?? undefined,
    feedbackType,
    reason,
    comment: storedComment.comment ?? feedbackComment ?? undefined,
    submitted: true,
    createdAt: message.feedback?.createdAt ?? message.feedbackCreatedAt ?? undefined,
  }
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
      messageId: message.messageId,
      chatRecordId: message.chatRecordId ?? undefined,
      conversationId: message.conversationId || detail.conversationId,
      knowledgeBaseId: detail.knowledgeBaseId ?? undefined,
      role,
      content: role === 'assistant' ? normalizeAnswer(content) : content,
      citations: role === 'assistant' ? normalizeSources(message.citations) : [],
      createdAt: message.createdAt,
      status: 'success' as const,
      feedback: role === 'assistant' ? mapFeedback(message) : undefined,
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
  const retryingMessageIdsRef = useRef<Set<string>>(new Set())
  const latestKnowledgeBaseIdRef = useRef<number | null>(null)
  const latestConversationIdRef = useRef<string | undefined>(undefined)

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState(routeKnowledgeBaseId ?? '')
  const [currentKnowledgeBase, setCurrentKnowledgeBase] = useState<KnowledgeBase | null>(null)
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [isInitialLoading, setIsInitialLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [sendingText, setSendingText] = useState('正在检索知识库...')
  const [isConversationListLoading, setIsConversationListLoading] = useState(false)
  const [isConversationDetailLoading, setIsConversationDetailLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [conversationListError, setConversationListError] = useState('')
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [retryingMessageIds, setRetryingMessageIds] = useState<Set<string>>(() => new Set())
  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [renamingConversation, setRenamingConversation] = useState<ConversationSummary | null>(null)
  const [renameTitle, setRenameTitle] = useState('')
  const [renameSubmitting, setRenameSubmitting] = useState(false)
  const [renameError, setRenameError] = useState('')
  const [deletingConversation, setDeletingConversation] = useState<ConversationSummary | null>(null)
  const [deleteSubmitting, setDeleteSubmitting] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const conversationId = useMemo(
    () => searchParams.get(CONVERSATION_QUERY_KEY)?.trim() || undefined,
    [searchParams],
  )
  const skipNextDetailLoadForConversationRef = useRef<string | undefined>(undefined)

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
  const isGlobalChat = !hasRouteKnowledgeBase
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

  const isSameKnowledgeBaseTarget = useCallback(
    (targetKnowledgeBaseId?: number | string | null) =>
      isGlobalChat
        ? targetKnowledgeBaseId === null || targetKnowledgeBaseId === undefined || targetKnowledgeBaseId === ''
        : String(latestKnowledgeBaseIdRef.current) === String(targetKnowledgeBaseId),
    [isGlobalChat],
  )

  const loadConversations = useCallback(
    async (knowledgeBaseId: number | null) => {
      setIsConversationListLoading(true)
      setConversationListError('')

      try {
        const response = await getConversations({
          knowledgeBaseId: knowledgeBaseId ?? undefined,
          page: DEFAULT_PAGE_NUM,
          size: DEFAULT_PAGE_SIZE,
        })

        if (latestKnowledgeBaseIdRef.current !== knowledgeBaseId) {
          return
        }

        if (response.code !== 0 || !response.data) {
          setConversations([])
          setConversationListError(response.message || '历史会话加载失败。')
          return
        }

        const nextConversations = conversationRecords(response.data).filter((conversation) =>
          knowledgeBaseId === null
            ? conversation.knowledgeBaseId === null || conversation.knowledgeBaseId === undefined
            : String(conversation.knowledgeBaseId) === String(knowledgeBaseId),
        )
        setConversations(nextConversations)
      } catch (error) {
        if (latestKnowledgeBaseIdRef.current !== knowledgeBaseId) {
          return
        }

        setConversations([])
        setConversationListError(getErrorMessage(error, '历史会话加载失败，请稍后重试。'))
      } finally {
        if (latestKnowledgeBaseIdRef.current === knowledgeBaseId) {
          setIsConversationListLoading(false)
        }
      }
    },
    [],
  )

  const loadConversationDetail = useCallback(
    async (nextConversationId: string) => {
      if (!isGlobalChat && selectedIdNumber === null) {
        return
      }

      const requestSeq = detailRequestSeqRef.current + 1
      detailRequestSeqRef.current = requestSeq
      setIsConversationDetailLoading(true)
      setErrorMessage('')

      try {
        const response = await getConversationDetail(nextConversationId)

        if (
          detailRequestSeqRef.current !== requestSeq ||
          latestConversationIdRef.current !== nextConversationId
        ) {
          return
        }

        if (response.code !== 0 || !response.data) {
          throw new Error(response.message || '会话详情加载失败。')
        }

        if (!isGlobalChat && String(response.data.knowledgeBaseId) !== String(selectedIdNumber)) {
          throw new Error('该会话不属于当前知识库。')
        }

        setMessages(mapConversationMessages(response.data))
      } catch (error) {
        if (
          detailRequestSeqRef.current !== requestSeq ||
          latestConversationIdRef.current !== nextConversationId
        ) {
          return
        }

        setErrorMessage(getErrorMessage(error, '会话详情加载失败，请稍后重试。'))
        setMessages([])
        latestConversationIdRef.current = undefined
        replaceConversationQuery(undefined)
      } finally {
        if (detailRequestSeqRef.current === requestSeq) {
          setIsConversationDetailLoading(false)
        }
      }
    },
    [isGlobalChat, replaceConversationQuery, selectedIdNumber],
  )

  useEffect(() => {
    if (routeKnowledgeBaseId) {
      setSelectedKnowledgeBaseId(routeKnowledgeBaseId)
    }
  }, [routeKnowledgeBaseId])

  useEffect(() => {
    async function loadKnowledgeBaseOptions() {
      if (isGlobalChat) {
        setKnowledgeBases([])
        setCurrentKnowledgeBase(null)
        setDocuments([])
        setSelectedKnowledgeBaseId('')
        setIsInitialLoading(false)
        return
      }

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
  }, [isGlobalChat])

  useEffect(() => {
    async function loadCurrentKnowledgeBase() {
      if (isGlobalChat) {
        setCurrentKnowledgeBase(null)
        setDocuments([])
        return
      }

      if (selectedIdNumber === null) {
        setCurrentKnowledgeBase(null)
        setDocuments([])
        setConversations([])
        latestConversationIdRef.current = undefined
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
  }, [isGlobalChat, selectedIdNumber])

  useEffect(() => {
    if (!isGlobalChat && selectedIdNumber === null) {
      return
    }

    setMessages([])
    setQuestion('')
    void loadConversations(isGlobalChat ? null : selectedIdNumber)
  }, [isGlobalChat, loadConversations, selectedIdNumber])

  useEffect(() => {
    if (!isGlobalChat && selectedIdNumber === null) {
      return
    }

    if (!conversationId) {
      return
    }

    if (skipNextDetailLoadForConversationRef.current === conversationId) {
      skipNextDetailLoadForConversationRef.current = undefined
      return
    }

    // URL restoration is intentionally best-effort; backend authorization remains authoritative.
    void loadConversationDetail(conversationId)
  }, [conversationId, isGlobalChat, loadConversationDetail, selectedIdNumber])

  useEffect(() => {
    listBottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, isSending, isConversationDetailLoading])

  useEffect(() => {
    if (!isSending) {
      setSendingText('正在检索知识库...')
      return
    }
    const timer = setTimeout(() => {
      setSendingText('正在生成回答...')
    }, 5000)
    return () => clearTimeout(timer)
  }, [isSending])

  function handleKnowledgeBaseChange(value: string) {
    setSelectedKnowledgeBaseId(value)
    latestConversationIdRef.current = undefined
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
    latestConversationIdRef.current = undefined
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

    latestConversationIdRef.current = nextConversationId
    detailRequestSeqRef.current += 1
    setMessages([])
    setQuestion('')
    replaceConversationQuery(nextConversationId)
  }

  async function handleTogglePin(targetConversation: ConversationSummary) {
    if (!targetConversation.conversationId) {
      setErrorMessage('缺少会话或知识库信息，无法置顶。')
      return
    }

    try {
      const response = await pinConversation(targetConversation.conversationId, {
        knowledgeBaseId: targetConversation.knowledgeBaseId,
        pinned: !targetConversation.pinned,
      })

      if (response.code !== 0) {
        throw new Error(response.message || '置顶操作失败，请稍后重试。')
      }

      const updatedConversation = response.data ?? {
        ...targetConversation,
        pinned: !targetConversation.pinned,
        pinnedAt: !targetConversation.pinned ? new Date().toISOString() : undefined,
      }

      setConversations((current) =>
        current.map((item) =>
          item.conversationId === targetConversation.conversationId
            ? { ...item, ...updatedConversation }
            : item,
        ),
      )

      void loadConversations(isGlobalChat ? null : selectedIdNumber)
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '置顶操作失败，请稍后重试。'))
    }
  }

  function handleOpenRename(nextConversation: ConversationSummary) {
    setRenamingConversation(nextConversation)
    setRenameTitle(nextConversation.title || '')
    setRenameError('')
  }

  function handleCloseRename() {
    if (renameSubmitting) {
      return
    }

    setRenamingConversation(null)
    setRenameTitle('')
    setRenameError('')
  }

  async function handleSaveRename() {
    if (!renamingConversation || renameSubmitting) {
      return
    }

    const trimmedTitle = renameTitle.trim()
    if (!trimmedTitle) {
      setRenameError('会话标题不能为空。')
      return
    }

    if (!renamingConversation.conversationId) {
      setRenameError('缺少会话或知识库信息，无法重命名。')
      return
    }

    setRenameSubmitting(true)
    setRenameError('')

    try {
      const response = await renameConversation(renamingConversation.conversationId, {
        knowledgeBaseId: renamingConversation.knowledgeBaseId,
        title: trimmedTitle,
      })

      if (response.code !== 0) {
        throw new Error(response.message || '重命名失败，请稍后重试。')
      }

      const updatedConversation = response.data ?? {
        ...renamingConversation,
        title: trimmedTitle,
      }

      setConversations((current) =>
        current.map((item) =>
          item.conversationId === renamingConversation.conversationId
            ? { ...item, ...updatedConversation, title: updatedConversation.title || trimmedTitle }
            : item,
        ),
      )
      setRenamingConversation(null)
      setRenameTitle('')
    } catch (error) {
      setRenameError(getErrorMessage(error, '重命名失败，请稍后重试。'))
    } finally {
      setRenameSubmitting(false)
    }
  }

  function handleOpenDelete(nextConversation: ConversationSummary) {
    setDeletingConversation(nextConversation)
    setDeleteError('')
  }

  function handleCloseDelete() {
    if (deleteSubmitting) {
      return
    }

    setDeletingConversation(null)
    setDeleteError('')
  }

  async function handleConfirmDelete() {
    if (!deletingConversation || deleteSubmitting) {
      return
    }

    if (!deletingConversation.conversationId) {
      setDeleteError('缺少会话或知识库信息，无法删除。')
      return
    }

    const deletingCurrentConversation = deletingConversation.conversationId === conversationId
    setDeleteSubmitting(true)
    setDeleteError('')

    try {
      const response = await deleteConversation(
        deletingConversation.conversationId,
        deletingConversation.knowledgeBaseId,
      )

      if (response.code !== 0) {
        throw new Error(response.message || '删除失败，请稍后重试。')
      }

      setConversations((current) =>
        current.filter((item) => item.conversationId !== deletingConversation.conversationId),
      )

      if (deletingCurrentConversation) {
        detailRequestSeqRef.current += 1
        latestConversationIdRef.current = undefined
        setMessages([])
        setQuestion('')
        setErrorMessage('')
        setIsConversationDetailLoading(false)
        replaceConversationQuery(undefined)
      }

      void loadConversations(isGlobalChat ? null : selectedIdNumber)

      setDeletingConversation(null)
    } catch (error) {
      setDeleteError(getErrorMessage(error, '删除失败，请稍后重试。'))
    } finally {
      setDeleteSubmitting(false)
    }
  }

  function updateConversationFromAnswer(answer: ChatAskResponse, sentQuestion: string) {
    const nextConversationId = answer.conversationId?.trim()
    if (!nextConversationId || (!isGlobalChat && selectedIdNumber === null)) {
      return
    }

    setConversations((current) => {
      const existing = current.find((conversation) => conversation.conversationId === nextConversationId)
      const updated: ConversationSummary = {
        conversationId: nextConversationId,
        knowledgeBaseId: isGlobalChat ? null : selectedIdNumber,
        scopeType: isGlobalChat ? 'ENTERPRISE_ALL' : 'KNOWLEDGE_BASE',
        title: existing?.title || truncateTitle(sentQuestion),
        messageCount: (existing?.messageCount ?? 0) + 2,
        lastQuestion: sentQuestion,
        lastAnswerPreview: normalizeAnswer(answer.answer).slice(0, 120),
        lastActiveAt: new Date().toISOString(),
        createdAt: existing?.createdAt,
        pinned: existing?.pinned,
        pinnedAt: existing?.pinnedAt,
      }

      return [updated, ...current.filter((conversation) => conversation.conversationId !== nextConversationId)]
    })
  }

  async function handleSubmitFeedback(message: ChatMessage, request: FeedbackSubmitRequest) {
    const requestConversationId = request.conversationId
    const requestKnowledgeBaseId = request.knowledgeBaseId
    const previousFeedback = message.feedback
    const shouldUpdate = previousFeedback?.submitted === true

    setMessages((current) =>
      current.map((item) =>
        item.id === message.id
          ? {
              ...item,
              feedback: {
                ...item.feedback,
                submitting: true,
                submittingType: request.feedbackType,
                error: undefined,
              },
            }
          : item,
      ),
    )

    try {
      const { response } = shouldUpdate ? await updateFeedback(request) : await submitFeedback(request)

      if (response.code !== 0) {
        throw new Error(response.message || '反馈提交失败，请稍后重试。')
      }

      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (!stillOnSameTarget) {
        return
      }

      setMessages((current) =>
        current.map((item) =>
          item.id === message.id
            ? {
                ...item,
                feedback: {
                  ...item.feedback,
                  feedbackType: request.feedbackType,
                  reason: request.reason,
                  comment: request.comment,
                  submitted: true,
                  submitting: false,
                  submittingType: undefined,
                  error: undefined,
                  createdAt: new Date().toISOString(),
                },
              }
            : item,
        ),
      )
    } catch (error) {
      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (!stillOnSameTarget) {
        return
      }

      setMessages((current) =>
        current.map((item) =>
          item.id === message.id
            ? {
                ...item,
                feedback: {
                  ...previousFeedback,
                  submitting: false,
                  submittingType: undefined,
                  error: getErrorMessage(error, '反馈提交失败，请稍后重试。'),
                },
              }
            : item,
        ),
      )
      throw error
    }
  }

  async function handleCancelFeedback(message: ChatMessage, request: FeedbackCancelRequest) {
    const requestConversationId = request.conversationId
    const requestKnowledgeBaseId = request.knowledgeBaseId
    const previousFeedback = message.feedback

    setMessages((current) =>
      current.map((item) =>
        item.id === message.id
          ? {
              ...item,
              feedback: {
                ...item.feedback,
                submitting: true,
                submittingType: item.feedback?.feedbackType,
                error: undefined,
              },
            }
          : item,
      ),
    )

    try {
      const { response } = await cancelFeedback(request)

      if (response.code !== 0) {
        throw new Error(response.message || '反馈撤回失败，请稍后重试。')
      }

      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (!stillOnSameTarget) {
        return
      }

      setMessages((current) =>
        current.map((item) =>
          item.id === message.id
            ? {
                ...item,
                feedback: {
                  submitted: false,
                  submitting: false,
                  submittingType: undefined,
                  error: undefined,
                },
              }
            : item,
        ),
      )
    } catch (error) {
      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (!stillOnSameTarget) {
        return
      }

      setMessages((current) =>
        current.map((item) =>
          item.id === message.id
            ? {
                ...item,
                feedback: {
                  ...previousFeedback,
                  submitting: false,
                  error: getErrorMessage(error, '反馈撤回失败，请稍后重试。'),
                },
              }
            : item,
        ),
      )
      throw error
    }
  }

  async function handleSend() {
    const trimmedQuestion = question.trim()

    if (!trimmedQuestion || isSending || (!isGlobalChat && selectedIdNumber === null)) {
      return
    }

    const requestKnowledgeBaseId = isGlobalChat ? null : selectedIdNumber
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
      const response = isGlobalChat
        ? await askGlobalChatQuestion({
            question: trimmedQuestion,
            // Existing sessions must reuse conversationId; new sessions omit it and let the backend create one.
            conversationId: requestConversationId,
          })
        : await askChatQuestion({
            knowledgeBaseId: requestKnowledgeBaseId,
            question: trimmedQuestion,
            conversationId: requestConversationId,
          })

      if (response.code !== 0 || !response.data) {
        throw new Error(response.message || '问答请求失败，请稍后重试。')
      }

      const chatResponse = response.data
      const nextConversationId = chatResponse.conversationId?.trim()
      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (isSameKnowledgeBaseTarget(requestKnowledgeBaseId)) {
        updateConversationFromAnswer(chatResponse, trimmedQuestion)
      }

      if (nextConversationId && stillOnSameTarget) {
        latestConversationIdRef.current = nextConversationId
        skipNextDetailLoadForConversationRef.current = nextConversationId
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
          id: chatResponse.messageId ?? createMessageId('assistant'),
          messageId: chatResponse.messageId ?? undefined,
          chatRecordId: chatResponse.chatRecordId ?? undefined,
          conversationId: nextConversationId,
          knowledgeBaseId: requestKnowledgeBaseId ?? undefined,
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
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
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

  async function handleRetry(message: ChatMessage, previousUserMessage?: ChatMessage) {
    const originalQuestion = previousUserMessage?.content?.trim()
    const requestKnowledgeBaseId = isGlobalChat ? null : Number(message.knowledgeBaseId ?? selectedIdNumber)
    const requestConversationId = message.conversationId ?? conversationId

    if (!originalQuestion) {
      setErrorMessage('无法找到原问题，不能重新生成。')
      return
    }

    if (!isGlobalChat && (requestKnowledgeBaseId == null || !Number.isInteger(requestKnowledgeBaseId) || requestKnowledgeBaseId <= 0)) {
      setErrorMessage('缺少知识库 ID，不能重新生成。')
      return
    }

    if (retryingMessageIdsRef.current.has(message.id) || isSending || message.feedback?.submitting) {
      return
    }

    const userMessageId = createMessageId('retry-user')

    retryingMessageIdsRef.current.add(message.id)
    setRetryingMessageIds(new Set(retryingMessageIdsRef.current))
    setMessages((current) => [
      ...current,
      {
        id: userMessageId,
        role: 'user',
        content: originalQuestion,
        conversationId: requestConversationId,
        knowledgeBaseId: requestKnowledgeBaseId ?? undefined,
        status: 'sending',
      },
    ])
    setErrorMessage('')

    try {
      const response = isGlobalChat
        ? await askGlobalChatQuestion({
            question: originalQuestion,
            conversationId: requestConversationId,
          })
        : await askChatQuestion({
            knowledgeBaseId: requestKnowledgeBaseId ?? undefined,
            question: originalQuestion,
            conversationId: requestConversationId,
          })

      if (response.code !== 0 || !response.data) {
        throw new Error(response.message || '重新生成失败，请稍后重试。')
      }

      const chatResponse = response.data
      const nextConversationId = chatResponse.conversationId?.trim() || requestConversationId
      const stillOnSameTarget =
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (isSameKnowledgeBaseTarget(requestKnowledgeBaseId)) {
        updateConversationFromAnswer(chatResponse, originalQuestion)
      }

      if (!stillOnSameTarget) {
        return
      }

      setMessages((current) => [
        ...current.map((item) =>
          item.id === userMessageId ? { ...item, status: 'success' as const } : item,
        ),
        {
          id: chatResponse.messageId ?? createMessageId('retry-assistant'),
          messageId: chatResponse.messageId ?? undefined,
          chatRecordId: chatResponse.chatRecordId ?? undefined,
          conversationId: nextConversationId,
          knowledgeBaseId: requestKnowledgeBaseId ?? undefined,
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
        isSameKnowledgeBaseTarget(requestKnowledgeBaseId) &&
        latestConversationIdRef.current === requestConversationId

      if (stillOnSameTarget) {
        setMessages((current) =>
          current.map((item) =>
            item.id === userMessageId ? { ...item, status: 'failed' as const } : item,
          ),
        )
        setErrorMessage(getErrorMessage(error, '重新生成失败，请稍后重试。'))
      }
    } finally {
      retryingMessageIdsRef.current.delete(message.id)
      setRetryingMessageIds(new Set(retryingMessageIdsRef.current))
    }
  }

  if (isInitialLoading) {
    return <div className="px-1 py-12 text-center text-sm text-slate-500">加载中...</div>
  }

  return (
    <>
    {/* Mobile sidebar drawer */}
    {sidebarOpen ? (
      <div className="fixed inset-0 z-50 md:hidden">
        <div className="absolute inset-0 bg-black/50" onClick={() => setSidebarOpen(false)} />
        <div className="absolute bottom-0 left-0 top-0 w-72 max-w-[82vw] bg-white">
          <ConversationSidebar
            conversations={conversations}
            currentConversationId={conversationId}
            loading={isConversationListLoading}
            errorMessage={conversationListError}
            onSelect={(id) => { handleSelectConversation(id); setSidebarOpen(false); }}
            onNewConversation={() => { handleNewConversation(); setSidebarOpen(false); }}
            onRename={handleOpenRename}
            onDelete={handleOpenDelete}
            onTogglePin={handleTogglePin}
            bottomContent={<UserMenu />}
          />
        </div>
      </div>
    ) : null}

    <section className="flex h-screen h-[100dvh] overflow-hidden">
      {/* Desktop sidebar */}
      <div className="hidden md:block md:w-72 md:shrink-0">
        <ConversationSidebar
          conversations={conversations}
          currentConversationId={conversationId}
          loading={isConversationListLoading}
          errorMessage={conversationListError}
          onSelect={handleSelectConversation}
          onNewConversation={handleNewConversation}
          onRename={handleOpenRename}
          onDelete={handleOpenDelete}
          onTogglePin={handleTogglePin}
          bottomContent={<UserMenu />}
        />
      </div>

      <main className="flex min-h-0 min-w-0 flex-1 flex-col bg-white">
      {/* Mobile header */}
      <div className="flex shrink-0 items-center justify-between border-b border-slate-200 px-3 py-2 md:hidden">
        <button
          type="button"
          onClick={() => setSidebarOpen(true)}
          className="inline-flex h-10 w-10 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100"
          aria-label="打开历史会话"
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
          </svg>
        </button>
        <span className="truncate text-sm font-semibold text-slate-900">AI Knowledge Base QA</span>
        <button
          type="button"
          onClick={handleNewConversation}
          className="inline-flex h-10 w-10 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100"
          aria-label="新建会话"
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
        </button>
      </div>

      {errorMessage ? (
        <div className="mx-4 mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 sm:mx-6">
          {errorMessage}
        </div>
      ) : null}

      {shouldShowEmbeddingHint ? (
        <div className="mx-4 mt-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 sm:mx-6">
          当前知识库尚未发现已完成 Embedding 的文档，请先在知识库详情页完成文档解析和 Embedding。
        </div>
      ) : null}

      <div className="min-h-0 flex-1">
        <div className="flex h-full min-h-0 flex-col overflow-hidden">
          <div className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6">
            <div className="mx-auto max-w-4xl">
            <ChatMessageList
              messages={messages}
              sending={isSending}
              sendingText={sendingText}
              detailLoading={isConversationDetailLoading}
              knowledgeBaseId={isGlobalChat ? undefined : selectedIdNumber ?? undefined}
              conversationId={conversationId}
              retryingMessageIds={retryingMessageIds}
              onSubmitFeedback={handleSubmitFeedback}
              onCancelFeedback={handleCancelFeedback}
              onRetry={handleRetry}
            />
            <div ref={listBottomRef} />
            </div>
          </div>

          <div className="shrink-0 border-t border-slate-200 bg-white px-4 py-4 shadow-[0_-8px_24px_rgba(15,23,42,0.04)] sm:px-6 pb-safe-bottom">
            <div className="mx-auto max-w-4xl">
            <ChatInput
              value={question}
              onChange={setQuestion}
              onSend={() => void handleSend()}
              sending={isSending}
              disabled={isSending || (!isGlobalChat && selectedIdNumber === null) || isConversationDetailLoading}
            />
            </div>
          </div>
        </div>
      </div>
      </main>
    </section>

    {renamingConversation ? (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="rename-conversation-title"
      >
        <div className="w-full max-w-sm rounded-md bg-white p-5 shadow-xl">
          <h2 id="rename-conversation-title" className="text-base font-semibold text-slate-950">
            重命名会话
          </h2>
          <label className="mt-4 block">
            <span className="text-sm font-medium text-slate-700">会话标题</span>
            <input
              value={renameTitle}
              onChange={(event) => setRenameTitle(event.target.value)}
              maxLength={100}
              autoFocus
              className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
            />
          </label>
          {renameError ? (
            <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {renameError}
            </div>
          ) : null}
          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={handleCloseRename}
              disabled={renameSubmitting}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              取消
            </button>
            <button
              type="button"
              onClick={() => void handleSaveRename()}
              disabled={renameSubmitting || !renameTitle.trim()}
              className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {renameSubmitting ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
      </div>
    ) : null}

    {deletingConversation ? (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-conversation-title"
      >
        <div className="w-full max-w-md rounded-md bg-white p-5 shadow-xl">
          <h2 id="delete-conversation-title" className="text-base font-semibold text-slate-950">
            确认删除会话？
          </h2>
          <p className="mt-3 break-words text-sm leading-6 text-slate-600">
            你确定要删除会话「{deletingConversation.title || '未命名会话'}」吗？删除后该会话中的历史消息将不再显示。
          </p>
          {deleteError ? (
            <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {deleteError}
            </div>
          ) : null}
          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={handleCloseDelete}
              disabled={deleteSubmitting}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              取消
            </button>
            <button
              type="button"
              onClick={() => void handleConfirmDelete()}
              disabled={deleteSubmitting}
              className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {deleteSubmitting ? '删除中...' : '确认删除'}
            </button>
          </div>
        </div>
      </div>
    ) : null}
    </>
  )
}
