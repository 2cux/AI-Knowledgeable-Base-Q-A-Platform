import { useState } from 'react'

import type { Citation } from '../../types/chat'

const SNIPPET_PREVIEW_LENGTH = 200

type SourceCardProps = {
  source: Citation & {
    originalIndex?: number
    relevanceLevel?: 'normal' | 'weak' | 'low'
  }
  displayIndex: number
}

function formatScore(score?: number | string | null) {
  if (typeof score !== 'number' || !Number.isFinite(score)) {
    return null
  }

  return score.toFixed(4)
}

export function SourceCard({ source, displayIndex }: SourceCardProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const content = source.contentSnippet || source.content || ''
  const shouldClamp = content.length > SNIPPET_PREVIEW_LENGTH
  const visibleContent = shouldClamp && !isExpanded
    ? `${content.slice(0, SNIPPET_PREVIEW_LENGTH)}...`
    : content
  const score = formatScore(source.score)
  const isWeak = source.relevanceLevel === 'weak' || source.relevanceLevel === 'low'

  return (
    <article
      className={[
        'rounded-md border px-3 py-2',
        isWeak
          ? 'border-slate-200 bg-slate-50 text-slate-500'
          : 'border-slate-200 bg-white text-slate-700',
      ].join(' ')}
    >
      <div className="flex flex-wrap items-center gap-2 text-xs">
        <span className="font-medium text-slate-700">来源 {displayIndex + 1}</span>
        <span className="font-medium text-slate-700">{source.documentName || '未知文档'}</span>
        {source.chunkIndex !== null && source.chunkIndex !== undefined ? (
          <span>片段 #{source.chunkIndex}</span>
        ) : source.chunkId ? (
          <span>片段 ID {source.chunkId}</span>
        ) : (
          <span>片段未知</span>
        )}
        {score ? <span>相关度 {score}</span> : <span>相关度未知</span>}
        {source.relevanceLevel === 'weak' ? (
          <span className="rounded bg-slate-200 px-1.5 py-0.5 text-[11px] text-slate-600">
            相关度一般
          </span>
        ) : null}
        {source.relevanceLevel === 'low' ? (
          <span className="rounded bg-amber-100 px-1.5 py-0.5 text-[11px] text-amber-700">
            相关度较低
          </span>
        ) : null}
      </div>

      {content ? (
        <div className="mt-2">
          <p className="whitespace-pre-wrap break-words text-xs leading-5 text-slate-600">
            {visibleContent}
          </p>
          {shouldClamp ? (
            <button
              type="button"
              onClick={() => setIsExpanded((current) => !current)}
              className="mt-1 text-xs font-medium text-slate-600 hover:text-slate-950"
            >
              {isExpanded ? '收起片段' : '展开片段'}
            </button>
          ) : null}
        </div>
      ) : null}
    </article>
  )
}
