import request from './request'
import type { ApiResponse } from '../types/auth'
import type { ChatAskRequest, ChatAskResponse } from '../types/chat'

const CHAT_PATH = '/api/chat'

export function askChatQuestion(data: ChatAskRequest) {
  return request.post<ApiResponse<ChatAskResponse>, ApiResponse<ChatAskResponse>>(
    `${CHAT_PATH}/ask`,
    data,
  )
}
