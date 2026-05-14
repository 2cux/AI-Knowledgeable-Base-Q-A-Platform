import axios from 'axios'
import type { ApiResponse } from '../types/auth'
import type { ChatAskRequest, ChatAskResponse } from '../types/chat'
import { getToken, removeToken } from '../utils/token'

const CHAT_PATH = '/chat'

// Dedicated axios instance for chat Q&A requests with extended timeout.
// The RAG pipeline (embedding + vector search + LLM call) can take 60s+,
// so the 30s global timeout is insufficient.
const chatRequest = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000,
})

chatRequest.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

chatRequest.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      window.dispatchEvent(new Event('aikb:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export function askChatQuestion(data: ChatAskRequest) {
  return chatRequest.post<ApiResponse<ChatAskResponse>, ApiResponse<ChatAskResponse>>(
    `${CHAT_PATH}/ask`,
    data,
  )
}

export function askGlobalChatQuestion(data: Omit<ChatAskRequest, 'knowledgeBaseId'>) {
  return chatRequest.post<ApiResponse<ChatAskResponse>, ApiResponse<ChatAskResponse>>(
    `${CHAT_PATH}/ask-global`,
    data,
  )
}
