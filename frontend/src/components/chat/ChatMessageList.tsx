import type { ChatAskResponse, ChatMessage } from '../../types/chat'
import type { FeedbackCancelRequest, FeedbackSubmitRequest } from '../../types/feedback'
import { normalizeSources } from '../../utils/chatSources'
import { AssistantMessageActions } from './AssistantMessageActions'
import { SourceList } from './SourceList'

type ChatMessageListProps = {
  messages: ChatMessage[]
  sending: boolean
  detailLoading: boolean
  knowledgeBaseId?: number | string
  conversationId?: string
  retryingMessageIds?: Set<string>
  onSubmitFeedback: (message: ChatMessage, request: FeedbackSubmitRequest) => Promise<void>
  onCancelFeedback: (message: ChatMessage, request: FeedbackCancelRequest) => Promise<void>
  onRetry: (message: ChatMessage, previousUserMessage?: ChatMessage) => Promise<void>
}

function ChatWelcome() {
  return (
    <div className="flex min-h-[22rem] items-center justify-center px-4 text-center">
      <div className="max-w-xl">
        <h2 className="text-2xl font-semibold text-slate-950">有什么我能帮到你吗？</h2>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          输入问题，开始基于当前知识库的问答。
        </p>
        <div className="mt-6 grid gap-2 text-left text-sm text-slate-600 sm:grid-cols-2">
          <div className="rounded-md border border-slate-200 bg-white px-3 py-2">询问当前知识库中的制度内容</div>
          <div className="rounded-md border border-slate-200 bg-white px-3 py-2">让系统总结文档重点</div>
          <div className="rounded-md border border-slate-200 bg-white px-3 py-2">查询某个流程或操作说明</div>
          <div className="rounded-md border border-slate-200 bg-white px-3 py-2">核对文档中的关键要求</div>
        </div>
      </div>
    </div>
  )
}

function ResponseMeta({
  message,
  knowledgeBaseId,
  conversationId,
  onSubmitFeedback,
  onCancelFeedback,
  onRetry,
  previousUserMessage,
  retrying,
  retryDisabled,
}: {
  message: ChatMessage
  knowledgeBaseId?: number | string
  conversationId?: string
  onSubmitFeedback: (message: ChatMessage, request: FeedbackSubmitRequest) => Promise<void>
  onCancelFeedback: (message: ChatMessage, request: FeedbackCancelRequest) => Promise<void>
  onRetry: (message: ChatMessage, previousUserMessage?: ChatMessage) => Promise<void>
  previousUserMessage?: ChatMessage
  retrying?: boolean
  retryDisabled?: boolean
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

      <AssistantMessageActions
        content={message.content}
        knowledgeBaseId={message.knowledgeBaseId ?? knowledgeBaseId}
        conversationId={message.conversationId ?? conversationId}
        messageId={message.messageId}
        chatRecordId={message.chatRecordId}
        feedback={message.feedback}
        retrying={retrying}
        retryDisabled={retryDisabled || !previousUserMessage}
        retryDisabledReason={!previousUserMessage ? '无法找到原问题' : undefined}
        onSubmit={(request) => onSubmitFeedback(message, request)}
        onCancel={(request) => onCancelFeedback(message, request)}
        onRetry={() => onRetry(message, previousUserMessage)}
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
  retryingMessageIds = new Set<string>(),
  onSubmitFeedback,
  onCancelFeedback,
  onRetry,
}: ChatMessageListProps) {
  if (detailLoading) {
    return (
      <div className="flex min-h-72 items-center justify-center text-center text-sm text-slate-500">
        正在加载会话消息...
      </div>
    )
  }

  if (messages.length === 0) {
    return <ChatWelcome />
  }

  return (
    <div className="space-y-5">
      {messages.map((message, index) => {
        const previousUserMessage =
          message.role === 'assistant'
            ? [...messages.slice(0, index)].reverse().find((item) => item.role === 'user')
            : undefined

        return (
        <div
          key={message.id}
          className={['flex', message.role === 'user' ? 'justify-end' : 'justify-start'].join(' ')}
        >
          <div
            className={[
              'max-w-[88%] rounded-md px-4 py-3 text-sm leading-6 sm:max-w-[82%]',
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
                onCancelFeedback={onCancelFeedback}
                onRetry={onRetry}
                previousUserMessage={previousUserMessage}
                retrying={retryingMessageIds.has(message.id)}
                retryDisabled={sending || Boolean(message.feedback?.submitting)}
              />
            ) : null}
            {message.role === 'user' && message.status === 'failed' ? (
              <div className="mt-2 text-xs text-red-600">发送失败，请稍后重试。</div>
            ) : null}
          </div>
        </div>
        )
      })}

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
