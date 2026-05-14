import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  FeedbackCancelRequest,
  FeedbackCancelResponse,
  FeedbackReason,
  FeedbackSubmitRequest,
  FeedbackSubmitResponse,
  FeedbackType,
} from '../types/feedback'

const CHAT_RECORD_PATH = '/chat/records'

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

  // The backend stores one comment field; keep the reason prefix for history replay.
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

function assertChatRecordId(chatRecordId: FeedbackSubmitRequest['chatRecordId'], action: string) {
  if (!chatRecordId) {
    throw new Error(`缺少问答记录 ID，无法${action}反馈。`)
  }
}

export async function submitFeedback(data: FeedbackSubmitRequest) {
  assertChatRecordId(data.chatRecordId, '提交')

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

export async function updateFeedback(data: FeedbackSubmitRequest) {
  assertChatRecordId(data.chatRecordId, '修改')

  const body: BackendFeedbackRequest = {
    feedbackType: toBackendFeedbackType(data.feedbackType),
    comment: compactComment(data.reason, data.comment),
  }

  const response = await request.put<ApiResponse<null>, ApiResponse<null>>(
    `${CHAT_RECORD_PATH}/${encodeURIComponent(String(data.chatRecordId))}/feedback`,
    body,
  )

  const result: FeedbackSubmitResponse = {
    submitted: response.code === 0,
  }

  return { response, result }
}

export async function cancelFeedback(data: FeedbackCancelRequest) {
  if (!data.chatRecordId) {
    throw new Error('缺少问答记录 ID，无法撤回反馈。')
  }

  const response = await request.delete<ApiResponse<null>, ApiResponse<null>>(
    `${CHAT_RECORD_PATH}/${encodeURIComponent(String(data.chatRecordId))}/feedback`,
  )

  const result: FeedbackCancelResponse = {
    cancelled: response.code === 0,
  }

  return { response, result }
}
