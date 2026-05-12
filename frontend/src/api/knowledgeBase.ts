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

export function getKnowledgeBaseById(id: number) {
  return request.get<ApiResponse<KnowledgeBase>, ApiResponse<KnowledgeBase>>(
    `${KNOWLEDGE_BASE_PATH}/${id}`,
  )
}

// 创建知识库，统一走 request 封装以自动携带鉴权 token。
export function createKnowledgeBase(data: CreateKnowledgeBaseRequest) {
  return request.post<ApiResponse<KnowledgeBase>, ApiResponse<KnowledgeBase>>(
    KNOWLEDGE_BASE_PATH,
    data,
  )
}

// 更新知识库基础信息，路径与后端 /api/kb/{id} 保持一致。
export function updateKnowledgeBase(id: number, data: UpdateKnowledgeBaseRequest) {
  return request.put<ApiResponse<KnowledgeBase>, ApiResponse<KnowledgeBase>>(
    `${KNOWLEDGE_BASE_PATH}/${id}`,
    data,
  )
}

// 删除知识库，后端执行逻辑删除，前端成功后刷新列表即可。
export function deleteKnowledgeBase(id: number) {
  return request.delete<ApiResponse<void>, ApiResponse<void>>(`${KNOWLEDGE_BASE_PATH}/${id}`)
}
