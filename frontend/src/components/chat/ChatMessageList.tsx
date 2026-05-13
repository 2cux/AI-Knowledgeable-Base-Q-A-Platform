import type { ChatAskResponse, ChatMessage } from '../../types/chat'
import type { FeedbackSubmitRequest } from '../../types/feedback'
import { normalizeSources } from '../../utils/chatSources'
import { FeedbackActions } from './FeedbackActions'
import { SourceList } from './SourceList'

type ChatMessageListProps = {
  messages: ChatMessage[]
  sending: boolean
  detailLoading: boolean
  knowledgeBaseId?: number | string
  conversationId?: string
  onSubmitFeedback: (message: ChatMessage, request: FeedbackSubmitRequest) => Promise<void>
}

function ResponseMeta({
  message,
  knowledgeBaseId,
  conversationId,
  onSubmitFeedback,
}: {
  message: ChatMessage
  knowledgeBaseId?: number | string
  conversationId?: string
  onSubmitFeedback: (message: ChatMessage, request: FeedbackSubmitRequest) => Promise<void>
}) {
  const response: ChatAskResponse | undefined = message.response
  const citations = message.citations ?? normalizeSources(response)
  const effectiveCount = response?.effectiveChunkCount ?? response?.retrievedChunkCount
  const rawCount = response?.rawRetrievedChunkCount
  const shouldWarn = response?.matched === false || effectiveCount === 0 || rawCount === 0

  return (
    <div className="mt-3 space-y-3">
      {message.status === 'failed' ? (
        <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
          发送失败，请稍后重试。
        </div>
      ) : null}

      {shouldWarn ? (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          未检索到明确相关内容，以下回答可能不可靠。
        </div>
      ) : null}

      {citations.length > 0 ? (
        <SourceList sources={citations} />
      ) : effectiveCount !== null && effectiveCount !== undefined ? (
        <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
          检索命中：{effectiveCount} 个有效切片
          {rawCount !== null && rawCount !== undefined ? `，原始召回 ${rawCount} 个切片` : ''}
        </div>
      ) : null}

      <FeedbackActions
        knowledgeBaseId={message.knowledgeBaseId ?? knowledgeBaseId}
        conversationId={message.conversationId ?? conversationId}
        messageId={message.messageId}
        chatRecordId={message.chatRecordId}
        feedback={message.feedback}
        onSubmit={(request) => onSubmitFeedback(message, request)}
      />
    </div>
  )
}

export function ChatMessageList({
  messages,
  sending,
  detailLoading,
  knowledgeBaseId,
  conversationId,
  onSubmitFeedback,
}: ChatMessageListProps) {
  if (detailLoading) {
    return (
      <div className="flex min-h-72 items-center justify-center text-center text-sm text-slate-500">
        正在加载会话消息...
      </div>
    )
  }

  if (messages.length === 0) {
    return (
      <div className="flex min-h-72 items-center justify-center text-center text-sm text-slate-500">
        输入问题开始新的会话
      </div>
    )
  }

  return (
    <div className="space-y-5">
      {messages.map((message) => (
        <div
          key={message.id}
          className={['flex', message.role === 'user' ? 'justify-end' : 'justify-start'].join(' ')}
        >
          <div
            className={[
              'max-w-[88%] rounded-md px-4 py-3 text-sm leading-6',
              message.role === 'user'
                ? message.status === 'failed'
                  ? 'border border-red-200 bg-red-50 text-red-800'
                  : 'bg-slate-900 text-white'
                : 'border border-slate-200 bg-white text-slate-800',
            ].join(' ')}
          >
            <div className="whitespace-pre-wrap break-words">{message.content}</div>
            {message.role === 'assistant' ? (
              <ResponseMeta
                message={message}
                knowledgeBaseId={knowledgeBaseId}
                conversationId={conversationId}
                onSubmitFeedback={onSubmitFeedback}
              />
            ) : null}
            {message.role === 'user' && message.status === 'failed' ? (
              <div className="mt-2 text-xs text-red-600">发送失败，请稍后重试。</div>
            ) : null}
          </div>
        </div>
      ))}

      {sending ? (
        <div className="flex justify-start">
          <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
            正在生成回答...
          </div>
        </div>
      ) : null}
    </div>
  )
}
