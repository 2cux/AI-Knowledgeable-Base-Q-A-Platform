import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  FeedbackReason,
  FeedbackSubmitRequest,
  FeedbackSubmitResponse,
  FeedbackType,
} from '../types/feedback'

const CHAT_RECORD_PATH = '/api/chat/records'

type BackendFeedbackType = 'LIKE' | 'DISLIKE'

type BackendFeedbackRequest = {
  feedbackType: BackendFeedbackType
  comment?: string
}

function toBackendFeedbackType(feedbackType: FeedbackType): BackendFeedbackType {
  return feedbackType === 'HELPFUL' ? 'LIKE' : 'DISLIKE'
}

export function toClientFeedbackType(feedbackType?: string | null): FeedbackType | undefined {
  if (feedbackType === 'HELPFUL' || feedbackType === 'LIKE') {
    return 'HELPFUL'
  }

  if (feedbackType === 'NOT_HELPFUL' || feedbackType === 'DISLIKE') {
    return 'NOT_HELPFUL'
  }

  return undefined
}

export function isFeedbackReason(value: unknown): value is FeedbackReason {
  return (
    value === 'ANSWER_ACCURATE' ||
    value === 'ANSWER_INACCURATE' ||
    value === 'SOURCE_NOT_RELEVANT' ||
    value === 'ANSWER_INCOMPLETE' ||
    value === 'HALLUCINATION' ||
    value === 'FORMAT_BAD' ||
    value === 'OTHER'
  )
}

function compactComment(reason?: FeedbackReason, comment?: string) {
  const trimmedComment = comment?.trim()

  if (!reason) {
    return trimmedComment || undefined
  }

  // The existing backend only persists feedbackType and comment, so the MVP stores
  // the selected reason as a compact comment prefix for historical replay.
  return trimmedComment ? `[${reason}] ${trimmedComment}` : `[${reason}]`
}

export function splitStoredFeedbackComment(comment?: string | null) {
  const trimmed = comment?.trim() ?? ''
  const match = trimmed.match(/^\[([A-Z_]+)]\s*(.*)$/)

  if (!match) {
    return { reason: undefined, comment: trimmed || undefined }
  }

  const reason = isFeedbackReason(match[1]) ? match[1] : undefined
  return {
    reason,
    comment: match[2]?.trim() || undefined,
  }
}

export async function submitFeedback(data: FeedbackSubmitRequest) {
  if (!data.chatRecordId) {
    throw new Error('缺少问答记录 ID，无法提交反馈。')
  }

  const body: BackendFeedbackRequest = {
    feedbackType: toBackendFeedbackType(data.feedbackType),
    comment: compactComment(data.reason, data.comment),
  }

  const response = await request.post<ApiResponse<null>, ApiResponse<null>>(
    `${CHAT_RECORD_PATH}/${encodeURIComponent(String(data.chatRecordId))}/feedback`,
    body,
  )

  const result: FeedbackSubmitResponse = {
    submitted: response.code === 0,
  }

  return { response, result }
}
