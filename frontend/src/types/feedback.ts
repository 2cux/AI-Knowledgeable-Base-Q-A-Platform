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
  knowledgeBaseId: number | string
  conversationId?: string
  messageId?: string
  chatRecordId?: number | string
  feedbackType: FeedbackType
  reason?: FeedbackReason
  comment?: string
}

export type FeedbackState = {
  feedbackId?: number | string
  feedbackType?: FeedbackType
  reason?: FeedbackReason
  comment?: string
  submitted?: boolean
  submitting?: boolean
  error?: string
  createdAt?: string
}

export type FeedbackSubmitResponse = {
  feedbackId?: number | string
  submitted: boolean
}
