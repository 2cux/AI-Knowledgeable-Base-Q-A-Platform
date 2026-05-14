import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  ConversationDetail,
  ConversationListResponse,
  ConversationPageParams,
  ConversationPinRequest,
  ConversationRenameRequest,
  ConversationSummary,
} from '../types/conversation'

const CONVERSATION_PATH = '/chat/conversations'

export function getConversations(params: ConversationPageParams) {
  return request.get<ApiResponse<ConversationListResponse>, ApiResponse<ConversationListResponse>>(
    CONVERSATION_PATH,
    { params },
  )
}

export function getConversationDetail(conversationId: string) {
  return request.get<ApiResponse<ConversationDetail>, ApiResponse<ConversationDetail>>(
    `${CONVERSATION_PATH}/${encodeURIComponent(conversationId)}`,
  )
}

export function renameConversation(conversationId: string, data: ConversationRenameRequest) {
  return request.patch<ApiResponse<ConversationSummary>, ApiResponse<ConversationSummary>>(
    `${CONVERSATION_PATH}/${encodeURIComponent(conversationId)}/title`,
    data,
  )
}

export function pinConversation(conversationId: string, data: ConversationPinRequest) {
  return request.patch<ApiResponse<ConversationSummary>, ApiResponse<ConversationSummary>>(
    `${CONVERSATION_PATH}/${encodeURIComponent(conversationId)}/pin`,
    data,
  )
}

export function deleteConversation(conversationId: string, knowledgeBaseId?: number | string | null) {
  const params = knowledgeBaseId === undefined || knowledgeBaseId === null ? undefined : { knowledgeBaseId }
  return request.delete<ApiResponse<void>, ApiResponse<void>>(
    `${CONVERSATION_PATH}/${encodeURIComponent(conversationId)}`,
    { params },
  )
}
