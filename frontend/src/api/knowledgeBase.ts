import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  CreateKnowledgeBaseRequest,
  KnowledgeBase,
  KnowledgeBaseListResponse,
  KnowledgeBasePageParams,
  UpdateKnowledgeBaseRequest,
} from '../types/knowledgeBase'

const KNOWLEDGE_BASE_PATH = '/api/kb'

export function getKnowledgeBases(params: KnowledgeBasePageParams = {}) {
  return request.get<ApiResponse<KnowledgeBaseListResponse>, ApiResponse<KnowledgeBaseListResponse>>(
    KNOWLEDGE_BASE_PATH,
    { params },
  )
}

export function createKnowledgeBase(data: CreateKnowledgeBaseRequest) {
  return request.post<ApiResponse<KnowledgeBase>, ApiResponse<KnowledgeBase>>(
    KNOWLEDGE_BASE_PATH,
    data,
  )
}

export function updateKnowledgeBase(id: number, data: UpdateKnowledgeBaseRequest) {
  return request.put<ApiResponse<KnowledgeBase>, ApiResponse<KnowledgeBase>>(
    `${KNOWLEDGE_BASE_PATH}/${id}`,
    data,
  )
}

export function deleteKnowledgeBase(id: number) {
  return request.delete<ApiResponse<void>, ApiResponse<void>>(`${KNOWLEDGE_BASE_PATH}/${id}`)
}
