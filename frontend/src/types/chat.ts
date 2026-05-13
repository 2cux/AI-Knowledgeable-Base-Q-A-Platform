export type Citation = {
  documentId?: number | string | null
  documentName?: string | null
  knowledgeBaseId?: number | string | null
  chunkId?: number | string | null
  chunkIndex?: number | null
  content?: string | null
  contentSnippet?: string | null
  score?: number | null
}

export type ChatAskRequest = {
  knowledgeBaseId: number | string
  question: string
  conversationId?: string
}

export type ChatAskResponse = {
  conversationId?: string | null
  chatRecordId?: number | string | null
  answer: string
  answerStatus?: string | null
  matched?: boolean | null
  retrievedChunkCount?: number | null
  rawRetrievedChunkCount?: number | null
  effectiveChunkCount?: number | null
  minEffectiveScore?: number | null
  citations?: Citation[] | null
  sources?: Citation[] | null
  chunks?: Citation[] | null
  retrievedChunks?: Citation[] | null
}
