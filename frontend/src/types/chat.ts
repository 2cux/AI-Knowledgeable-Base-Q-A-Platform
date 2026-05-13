import type { FeedbackState } from './feedback'

export type Citation = {
  documentId?: number | string | null
  documentName?: string | null
  knowledgeBaseId?: number | string | null
  chunkId?: number | string | null
  chunkIndex?: number | null
  content?: string | null
  contentSnippet?: string | null
  score?: number | string | null
}

export type ChatAskRequest = {
  knowledgeBaseId: number | string
  question: string
  conversationId?: string
}

export type ChatAskResponse = {
  conversationId?: string | null
  chatRecordId?: number | string | null
  answer?: string | null
  answerStatus?: string | null
  matched?: boolean | null
  retrievedChunkCount?: number | null
  rawRetrievedChunkCount?: number | null
  effectiveChunkCount?: number | null
  minEffectiveScore?: number | null
  citations?: Citation[] | string | null
  sources?: Citation[] | string | null
  chunks?: Citation[] | string | null
  retrievedChunks?: Citation[] | string | null
}

export type ChatMessage = {
  id: string
  messageId?: string
  chatRecordId?: number | string
  conversationId?: string
  knowledgeBaseId?: number | string
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  createdAt?: string
  status?: 'sending' | 'success' | 'failed'
  response?: ChatAskResponse
  feedback?: FeedbackState
}
