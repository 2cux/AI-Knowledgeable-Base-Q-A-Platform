export type FeedbackType = 'HELPFUL' | 'NOT_HELPFUL'

export type FeedbackReason =
  | 'ANSWER_ACCURATE'
  | 'ANSWER_INACCURATE'
  | 'SOURCE_NOT_RELEVANT'
  | 'ANSWER_INCOMPLETE'
  | 'HALLUCINATION'
  | 'FORMAT_BAD'
  | 'OTHER'

export type FeedbackSubmitRequest = {
  knowledgeBaseId?: number | string | null
  conversationId?: string
  messageId?: string
  chatRecordId?: number | string
  feedbackType: FeedbackType
  reason?: FeedbackReason
  comment?: string
}

export type FeedbackCancelRequest = {
  knowledgeBaseId?: number | string | null
  conversationId?: string
  messageId?: string
  chatRecordId?: number | string
  feedbackId?: number | string
}

export type FeedbackState = {
  feedbackId?: number | string
  feedbackType?: FeedbackType
  reason?: FeedbackReason
  comment?: string
  submitted?: boolean
  submitting?: boolean
  submittingType?: FeedbackType
  error?: string
  createdAt?: string
}

export type FeedbackSubmitResponse = {
  feedbackId?: number | string
  submitted: boolean
}

export type FeedbackCancelResponse = {
  cancelled: boolean
}
