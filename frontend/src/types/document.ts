export type DocumentParseStatus =
  | 'NOT_STARTED'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING'
  | 'RUNNING'
  | string

export type DocumentEmbeddingStatus =
  | 'NOT_STARTED'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING'
  | 'RUNNING'
  | string

export type KnowledgeDocument = {
  id: number
  documentId?: number | null
  knowledgeBaseId: number
  fileName: string
  originalFileName?: string | null
  fileType?: string | null
  fileSize?: number | null
  parseStatus?: DocumentParseStatus | null
  chunkCount?: number | null
  embeddingStatus?: DocumentEmbeddingStatus | null
  embeddedChunkCount?: number | null
  latestTaskStatus?: string | null
  latestErrorMessage?: string | null
  canReprocess?: boolean | null
  canReembed?: boolean | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type DocumentUploadResponse = KnowledgeDocument & {
  storagePath?: string | null
  createdBy?: number | null
}

export type DocumentProcessResponse = {
  documentId?: number | null
  knowledgeBaseId?: number | null
  chunkCount?: number | null
  parseStatus?: string | null
  taskId?: number | string | null
}

export type DocumentEmbeddingResponse = {
  documentId?: number | null
  knowledgeBaseId?: number | null
  total?: number | null
  successCount?: number | null
  failedCount?: number | null
  embeddingStatus?: string | null
  embeddingModel?: string | null
  taskId?: number | string | null
  taskStatus?: string | null
}

export type DocumentEmbeddingProgress = {
  documentId: number
  status: string
  totalChunks: number
  embeddedChunks: number
  progress: number
  errorMessage?: string | null
}
