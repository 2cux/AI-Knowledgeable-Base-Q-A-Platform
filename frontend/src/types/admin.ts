export type AdminAnswerStatus =
  | 'SUCCESS'
  | 'NO_HIT'
  | 'WEAK_HIT'
  | 'LLM_UNAVAILABLE'
  | 'RETRIEVAL_UNAVAILABLE'
  | string

export type AdminChatRecordListItem = {
  id: number
  userId?: number
  knowledgeBaseId: number
  conversationId?: string | null
  question: string
  answerPreview: string
  answerStatus?: AdminAnswerStatus | null
  matched?: boolean | null
  retrievedChunkCount?: number | null
  rawRetrievedChunkCount?: number | null
  topK?: number | null
  createdAt?: string | null
}

export type AdminCitation = {
  documentId?: number | null
  documentName?: string | null
  chunkId?: number | null
  chunkIndex?: number | null
  score?: number | null
  contentSnippet?: string | null
}

export type AdminChatRecordDetail = AdminChatRecordListItem & {
  answer: string
  citations: AdminCitation[]
}

export type AdminDashboard = {
  knowledgeBaseCount: number
  documentCount: number
  chatRecordCount: number
  feedbackCount: number
  unmatchedQuestionCount: number
  recentChatRecords: AdminChatRecordListItem[]
}

export type AdminFeedbackItem = {
  id: number
  chatRecordId?: number | null
  userId?: number | null
  knowledgeBaseId?: number | null
  conversationId?: string | null
  messageId?: string | null
  question?: string | null
  answerPreview?: string | null
  rating?: string | null
  feedbackType?: string | null
  reason?: string | null
  comment?: string | null
  handled?: boolean | null
  createdAt?: string | null
}

export type AdminUnmatchedQuestion = {
  id: number
  userId?: number
  knowledgeBaseId: number
  conversationId?: string | null
  question: string
  answerPreview?: string | null
  answerStatus?: AdminAnswerStatus | null
  matched?: boolean | null
  retrievedChunkCount?: number | null
  rawRetrievedChunkCount?: number | null
  topK?: number | null
  createdAt?: string | null
}

export type AdminPage<T> = {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export type AdminListParams = {
  page?: number
  pageSize?: number
  keyword?: string
  knowledgeBaseId?: string
  matched?: string
  answerStatus?: string
  feedbackType?: string
  reason?: string
  handled?: string
}
