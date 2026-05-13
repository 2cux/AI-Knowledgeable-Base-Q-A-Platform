import type { ConversationSummary } from '../../types/conversation'
import { ConversationListItem } from './ConversationListItem'

type ConversationListProps = {
  conversations: ConversationSummary[]
  currentConversationId?: string
  loading: boolean
  errorMessage?: string
  onSelect: (conversationId: string) => void
}

export function ConversationList({
  conversations,
  currentConversationId,
  loading,
  errorMessage,
  onSelect,
}: ConversationListProps) {
  if (loading) {
    return <div className="px-3 py-8 text-center text-sm text-slate-500">正在加载历史会话...</div>
  }

  if (errorMessage) {
    return (
      <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-3 text-sm text-amber-800">
        {errorMessage}
      </div>
    )
  }

  if (conversations.length === 0) {
    return <div className="px-3 py-8 text-center text-sm text-slate-500">暂无历史会话</div>
  }

  return (
    <div className="space-y-2">
      {conversations.map((conversation) => (
        <ConversationListItem
          key={conversation.conversationId}
          conversation={conversation}
          selected={conversation.conversationId === currentConversationId}
          onSelect={onSelect}
        />
      ))}
    </div>
  )
}
