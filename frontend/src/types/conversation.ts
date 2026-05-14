import type { Citation } from './chat'
import type { FeedbackReason, FeedbackState, FeedbackType } from './feedback'

export type ConversationSummary = {
  conversationId: string
  knowledgeBaseId?: number | string | null
  scopeType?: string | null
  title: string
  messageCount?: number
  lastQuestion?: string
  lastAnswerPreview?: string
  lastActiveAt?: string
  createdAt?: string
  pinned?: boolean
  pinnedAt?: string
}

export type ConversationMessage = {
  messageId?: string
  conversationId?: string
  role: 'USER' | 'ASSISTANT' | 'user' | 'assistant'
  content: string
  citations?: Citation[] | string | null
  chatRecordId?: number | string | null
  createdAt?: string
  answerStatus?: string
  feedback?: FeedbackState | null
  feedbackId?: number | string | null
  feedbackType?: FeedbackType | 'LIKE' | 'DISLIKE' | null
  feedbackReason?: FeedbackReason | null
  feedbackComment?: string | null
  feedbackCreatedAt?: string | null
}

export type ConversationDetail = {
  conversationId: string
  knowledgeBaseId?: number | string | null
  scopeType?: string | null
  title?: string
  createdAt?: string
  lastActiveAt?: string
  messages: ConversationMessage[]
}

export type ConversationListResponse = {
  list?: ConversationSummary[]
  records?: ConversationSummary[]
  total: number
  pageNum?: number
  pageSize?: number
}

export type ConversationPageParams = {
  knowledgeBaseId?: number | string
  page?: number
  size?: number
}

export type ConversationRenameRequest = {
  knowledgeBaseId?: number | string | null
  title: string
}

export type ConversationPinRequest = {
  knowledgeBaseId?: number | string | null
  pinned: boolean
}
