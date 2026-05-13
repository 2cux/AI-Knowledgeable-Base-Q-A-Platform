import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  ConversationDetail,
  ConversationListResponse,
  ConversationPageParams,
} from '../types/conversation'

const CONVERSATION_PATH = '/api/chat/conversations'

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
