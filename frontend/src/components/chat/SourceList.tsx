import { useMemo, useState } from 'react'

import type { Citation } from '../../types/chat'
import { getDisplaySources } from '../../utils/chatSources'
import { SourceCard } from './SourceCard'

const DEFAULT_VISIBLE_SOURCE_COUNT = 3

type SourceListProps = {
  sources: Citation[]
}

export function SourceList({ sources }: SourceListProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const displaySources = useMemo(() => getDisplaySources(sources), [sources])

  if (displaySources.length === 0) {
    return null
  }

  const visibleSources = isExpanded
    ? displaySources
    : displaySources.slice(0, DEFAULT_VISIBLE_SOURCE_COUNT)
  const hasMoreSources = displaySources.length > DEFAULT_VISIBLE_SOURCE_COUNT

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs font-medium text-slate-500">引用来源</div>
        {hasMoreSources ? (
          <button
            type="button"
            onClick={() => setIsExpanded((current) => !current)}
            className="text-xs font-medium text-slate-500 hover:text-slate-900"
          >
            {isExpanded ? '收起来源' : `展开更多来源 (${displaySources.length})`}
          </button>
        ) : null}
      </div>

      <div className="grid gap-2 md:grid-cols-2">
        {visibleSources.map((source, index) => (
          <SourceCard
            key={`${source.documentId ?? 'doc'}-${source.chunkId ?? source.chunkIndex ?? source.originalIndex ?? index}`}
            source={source}
            displayIndex={index}
          />
        ))}
      </div>
    </div>
  )
}
