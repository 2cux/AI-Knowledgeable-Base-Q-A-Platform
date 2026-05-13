import type { ConversationSummary } from '../../types/conversation'
import { ConversationList } from './ConversationList'

type ConversationSidebarProps = {
  conversations: ConversationSummary[]
  currentConversationId?: string
  loading: boolean
  errorMessage?: string
  onSelect: (conversationId: string) => void
  onNewConversation: () => void
}

export function ConversationSidebar({
  conversations,
  currentConversationId,
  loading,
  errorMessage,
  onSelect,
  onNewConversation,
}: ConversationSidebarProps) {
  return (
    <aside className="flex max-h-80 flex-col rounded-md border border-slate-200 bg-white shadow-sm lg:max-h-none lg:w-80 lg:shrink-0">
      <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
        <h2 className="text-sm font-semibold text-slate-900">历史会话</h2>
        <button
          type="button"
          onClick={onNewConversation}
          className="rounded bg-slate-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700"
        >
          新建会话
        </button>
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto p-3">
        <ConversationList
          conversations={conversations}
          currentConversationId={currentConversationId}
          loading={loading}
          errorMessage={errorMessage}
          onSelect={onSelect}
        />
      </div>
    </aside>
  )
}
