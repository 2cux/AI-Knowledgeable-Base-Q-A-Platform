import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  DocumentEmbeddingProgress,
  DocumentEmbeddingResponse,
  DocumentProcessResponse,
  DocumentUploadResponse,
  KnowledgeDocument,
} from '../types/document'

const KNOWLEDGE_BASE_PATH = '/kb'
const DOCUMENT_PATH = '/documents'

export function getDocumentsByKnowledgeBaseId(knowledgeBaseId: number) {
  return request.get<ApiResponse<KnowledgeDocument[]>, ApiResponse<KnowledgeDocument[]>>(
    `${KNOWLEDGE_BASE_PATH}/${knowledgeBaseId}/documents`,
  )
}

export function uploadDocument(knowledgeBaseId: number, file: File) {
  const formData = new FormData()
  formData.append('knowledgeBaseId', String(knowledgeBaseId))
  formData.append('file', file)
  formData.append('fileName', file.name)

  return request.post<ApiResponse<DocumentUploadResponse>, ApiResponse<DocumentUploadResponse>>(
    `${DOCUMENT_PATH}/upload-file`,
    formData,
  )
}

export function processDocument(documentId: number) {
  return request.post<ApiResponse<DocumentProcessResponse>, ApiResponse<DocumentProcessResponse>>(
    `${DOCUMENT_PATH}/${documentId}/process`,
    {},
  )
}

export function embedDocument(documentId: number) {
  return request.post<ApiResponse<DocumentEmbeddingResponse>, ApiResponse<DocumentEmbeddingResponse>>(
    `${DOCUMENT_PATH}/${documentId}/embed`,
    {},
  )
}

export function deleteDocument(documentId: number) {
  return request.delete<ApiResponse<void>, ApiResponse<void>>(
    `${DOCUMENT_PATH}/${documentId}`,
  )
}

export function getEmbeddingProgress(documentId: number) {
  return request.get<ApiResponse<DocumentEmbeddingProgress>, ApiResponse<DocumentEmbeddingProgress>>(
    `${DOCUMENT_PATH}/${documentId}/embedding/progress`,
  )
}
