import request from './request'
import type { ApiResponse } from '../types/auth'
import type { ChatAskRequest, ChatAskResponse } from '../types/chat'

const CHAT_PATH = '/chat'

export function askChatQuestion(data: ChatAskRequest) {
  return request.post<ApiResponse<ChatAskResponse>, ApiResponse<ChatAskResponse>>(
    `${CHAT_PATH}/ask`,
    data,
  )
}

export function askGlobalChatQuestion(data: Omit<ChatAskRequest, 'knowledgeBaseId'>) {
  return request.post<ApiResponse<ChatAskResponse>, ApiResponse<ChatAskResponse>>(
    `${CHAT_PATH}/ask-global`,
    data,
  )
}
