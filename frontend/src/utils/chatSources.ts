import type { ChatAskResponse, Citation } from '../types/chat'

type SourceFieldName = 'citations' | 'sources' | 'chunks' | 'retrievedChunks'
type SourceFieldValue = ChatAskResponse[SourceFieldName]
type SourceContainer = Partial<Record<SourceFieldName, SourceFieldValue>>

const SOURCE_FIELD_PRIORITY: SourceFieldName[] = [
  'citations',
  'sources',
  'chunks',
  'retrievedChunks',
]

const LOW_SCORE_THRESHOLD = 0.25
const WEAK_SCORE_THRESHOLD = 0.35
const LOW_SCORE_FALLBACK_LIMIT = 3

function hasText(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

function isSourceContainer(value: unknown): value is SourceContainer {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return false
  }

  const record = value as Record<string, unknown>
  return SOURCE_FIELD_PRIORITY.some((field) => field in record)
}

function readArrayInput(input: unknown): unknown[] {
  if (Array.isArray(input)) {
    return input
  }

  if (hasText(input)) {
    try {
      const parsed = JSON.parse(input) as unknown
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }

  return []
}

function readString(value: unknown): string | undefined {
  return hasText(value) ? value.trim() : undefined
}

function readId(value: unknown): number | string | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (hasText(value)) {
    return value.trim()
  }

  return undefined
}

function readNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (hasText(value)) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }

  return undefined
}

function normalizeSourceItem(value: unknown): Citation | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null
  }

  const record = value as Record<string, unknown>
  const documentName = readString(record.documentName)
  const contentSnippet = readString(record.contentSnippet)
  const content = readString(record.content)

  if (!documentName && !contentSnippet && !content) {
    return null
  }

  return {
    chunkId: readId(record.chunkId),
    documentId: readId(record.documentId),
    knowledgeBaseId: readId(record.knowledgeBaseId),
    knowledgeBaseName: readString(record.knowledgeBaseName),
    chunkIndex: readNumber(record.chunkIndex),
    documentName,
    score: readNumber(record.score),
    contentSnippet,
    content,
  }
}

function normalizeArrayInput(input: unknown): Citation[] {
  return readArrayInput(input).reduce<Citation[]>((sources, item) => {
    const source = normalizeSourceItem(item)
    if (source) {
      sources.push(source)
    }
    return sources
  }, [])
}

export function normalizeSources(input: unknown): Citation[] {
  if (isSourceContainer(input)) {
    for (const field of SOURCE_FIELD_PRIORITY) {
      const normalized = normalizeArrayInput(input[field])
      if (normalized.length > 0) {
        return normalized
      }
    }

    return []
  }

  return normalizeArrayInput(input)
}

export function getDisplaySources(sources: Citation[]) {
  const scoredSources = sources.map((source, originalIndex) => ({
    source,
    originalIndex,
    numericScore: readNumber(source.score),
  }))

  const strongEnough = scoredSources.filter(
    (item) => item.numericScore === undefined || item.numericScore >= LOW_SCORE_THRESHOLD,
  )

  // Low-score filtering is presentation-only: retrieval results are preserved on the message.
  if (strongEnough.length > 0) {
    return strongEnough.map((item) => ({
      ...item.source,
      relevanceLevel:
        item.numericScore !== undefined && item.numericScore < WEAK_SCORE_THRESHOLD
          ? ('weak' as const)
          : ('normal' as const),
      originalIndex: item.originalIndex,
    }))
  }

  return scoredSources
    .sort((first, second) => (second.numericScore ?? -Infinity) - (first.numericScore ?? -Infinity))
    .slice(0, LOW_SCORE_FALLBACK_LIMIT)
    .sort((first, second) => first.originalIndex - second.originalIndex)
    .map((item) => ({
      ...item.source,
      relevanceLevel: 'low' as const,
      originalIndex: item.originalIndex,
    }))
}
