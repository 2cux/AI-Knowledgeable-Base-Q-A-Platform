export type PageResult<T> = {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export type KnowledgeBase = {
  id: number
  name: string
  description?: string | null
  status?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type KnowledgeBaseListResponse = PageResult<KnowledgeBase>

export type KnowledgeBasePageParams = {
  pageNum?: number
  pageSize?: number
}

export type CreateKnowledgeBaseRequest = {
  name: string
  description?: string
}

export type UpdateKnowledgeBaseRequest = {
  name: string
  description?: string
}
