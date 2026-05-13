import type { ConversationSummary } from '../../types/conversation'

type ConversationListItemProps = {
  conversation: ConversationSummary
  selected: boolean
  onSelect: (conversationId: string) => void
}

function formatTime(value?: string) {
  if (!value) {
    return '无时间'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ConversationListItem({
  conversation,
  selected,
  onSelect,
}: ConversationListItemProps) {
  const summary = conversation.lastQuestion || conversation.lastAnswerPreview || '暂无摘要'

  return (
    <button
      type="button"
      onClick={() => onSelect(conversation.conversationId)}
      className={[
        'w-full rounded-md border px-3 py-3 text-left transition',
        selected
          ? 'border-slate-900 bg-slate-900 text-white'
          : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50',
      ].join(' ')}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1 truncate text-sm font-medium">
          {conversation.title || '未命名会话'}
        </div>
        <div className={selected ? 'shrink-0 text-xs text-slate-300' : 'shrink-0 text-xs text-slate-500'}>
          {conversation.messageCount ?? 0} 条
        </div>
      </div>
      <div className={selected ? 'mt-2 truncate text-xs text-slate-200' : 'mt-2 truncate text-xs text-slate-500'}>
        {summary}
      </div>
      <div className={selected ? 'mt-2 text-xs text-slate-300' : 'mt-2 text-xs text-slate-400'}>
        {formatTime(conversation.lastActiveAt || conversation.createdAt)}
      </div>
    </button>
  )
}
