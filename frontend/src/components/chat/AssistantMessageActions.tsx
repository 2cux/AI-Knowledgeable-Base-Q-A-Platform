import { useState } from 'react'
import { splitStoredFeedbackComment } from '../../api/feedback'
import type {
  FeedbackCancelRequest,
  FeedbackReason,
  FeedbackState,
  FeedbackSubmitRequest,
  FeedbackType,
} from '../../types/feedback'
import { FeedbackPanel } from './FeedbackPanel'

type AssistantMessageActionsProps = {
  content: string
  knowledgeBaseId?: number | string
  conversationId?: string
  messageId?: string
  chatRecordId?: number | string
  feedback?: FeedbackState
  retrying?: boolean
  retryDisabled?: boolean
  retryDisabledReason?: string
  onSubmit: (request: FeedbackSubmitRequest) => Promise<void>
  onCancel: (request: FeedbackCancelRequest) => Promise<void>
  onRetry: () => Promise<void> | void
}

type IconProps = {
  className?: string
}

function CopyIcon({ className = '' }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden="true">
      <rect x="9" y="9" width="10" height="10" rx="2" />
      <path d="M5 15V7a2 2 0 0 1 2-2h8" />
    </svg>
  )
}

function ThumbsUpIcon({ className = '' }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden="true">
      <path d="M7 10v10" />
      <path d="M7 11 11.5 4.5a1.8 1.8 0 0 1 3.3 1.1L14 10h4.2a2 2 0 0 1 2 2.3l-.8 5.2a3 3 0 0 1-3 2.5H7" />
      <path d="M3 10h4v10H3z" />
    </svg>
  )
}

function ThumbsDownIcon({ className = '' }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden="true">
      <path d="M17 14V4" />
      <path d="M17 13 12.5 19.5a1.8 1.8 0 0 1-3.3-1.1L10 14H5.8a2 2 0 0 1-2-2.3l.8-5.2a3 3 0 0 1 3-2.5H17" />
      <path d="M17 4h4v10h-4z" />
    </svg>
  )
}

function RefreshIcon({ className = '' }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden="true">
      <path d="M20 6v5h-5" />
      <path d="M4 18v-5h5" />
      <path d="M18.6 9A7 7 0 0 0 6.8 6.2L4 9" />
      <path d="M5.4 15A7 7 0 0 0 17.2 17.8L20 15" />
    </svg>
  )
}

function SpinnerIcon({ className = '' }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={`${className} animate-spin`} aria-hidden="true">
      <path d="M21 12a9 9 0 0 1-9 9" />
      <path d="M3 12a9 9 0 0 1 9-9" />
    </svg>
  )
}

function unavailableReason({
  conversationId,
  chatRecordId,
}: Pick<AssistantMessageActionsProps, 'knowledgeBaseId' | 'conversationId' | 'chatRecordId'>) {
  if (!conversationId) {
    return '缺少会话 ID，暂时无法反馈。'
  }

  if (!chatRecordId) {
    return '缺少问答记录 ID，暂时无法反馈。'
  }

  return ''
}

function actionButtonClass(selected = false) {
  return [
    'inline-flex h-8 w-8 items-center justify-center rounded-md border border-transparent transition',
    selected
      ? 'bg-slate-900 text-white hover:bg-slate-800 hover:text-white'
      : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900',
    'disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent disabled:hover:text-slate-500',
  ].join(' ')
}

function getReadableCopyText(content: string) {
  return content.trim()
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textArea = document.createElement('textarea')
  textArea.value = text
  textArea.setAttribute('readonly', 'true')
  textArea.style.position = 'fixed'
  textArea.style.left = '-9999px'
  textArea.style.top = '0'
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()

  try {
    const copied = document.execCommand('copy')
    if (!copied) {
      throw new Error('copy command failed')
    }
  } finally {
    document.body.removeChild(textArea)
  }
}

export function AssistantMessageActions({
  content,
  knowledgeBaseId,
  conversationId,
  messageId,
  chatRecordId,
  feedback,
  retrying = false,
  retryDisabled = false,
  retryDisabledReason,
  onSubmit,
  onCancel,
  onRetry,
}: AssistantMessageActionsProps) {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [copyStatus, setCopyStatus] = useState<'idle' | 'success' | 'error' | 'empty'>('idle')
  const disabledReason = unavailableReason({ knowledgeBaseId, conversationId, chatRecordId })
  const submitting = Boolean(feedback?.submitting)
  const submitted = feedback?.submitted === true
  const selectedType = submitted ? feedback?.feedbackType : undefined
  const pendingType = submitting ? feedback?.submittingType ?? selectedType : undefined
  const visibleType = pendingType ?? selectedType
  const showHelpful = !visibleType || visibleType === 'HELPFUL'
  const showNotHelpful = !visibleType || visibleType === 'NOT_HELPFUL'
  const stored = splitStoredFeedbackComment(feedback?.comment)
  const initialReason = feedback?.reason ?? stored.reason ?? 'ANSWER_INACCURATE'
  const initialComment = feedback?.comment && stored.reason ? stored.comment : feedback?.comment ?? ''
  const copyableText = getReadableCopyText(content)
  const copyDisabled = copyableText.length === 0
  const retryTitle = retryDisabledReason || (retrying ? '正在重新生成' : '重新生成')

  function baseRequest() {
    return {
      knowledgeBaseId: knowledgeBaseId as number | string,
      conversationId,
      messageId,
      chatRecordId,
    }
  }

  async function handleCopy() {
    if (copyDisabled) {
      setCopyStatus('empty')
      window.setTimeout(() => setCopyStatus('idle'), 1800)
      return
    }

    try {
      await copyText(copyableText)
      setCopyStatus('success')
    } catch {
      setCopyStatus('error')
    } finally {
      window.setTimeout(() => setCopyStatus('idle'), 1800)
    }
  }

  async function submit(feedbackType: FeedbackType, reason: FeedbackReason, comment?: string) {
    if (disabledReason || submitting) {
      return
    }

    try {
      await onSubmit({
        ...baseRequest(),
        feedbackType,
        reason,
        comment,
      })
      setDialogOpen(false)
    } catch {
      // The parent keeps the error on this message so the dialog input stays retryable.
    }
  }

  async function cancel() {
    if (disabledReason || submitting) {
      return
    }

    try {
      await onCancel({
        ...baseRequest(),
        feedbackId: feedback?.feedbackId,
      })
      setDialogOpen(false)
    } catch {
      // The parent restores the previous feedback state on failure.
    }
  }

  return (
    <div className="pt-1">
      <div className="flex flex-wrap items-center justify-end gap-2">
        <div className="min-h-5 text-xs text-slate-400" role="status" aria-live="polite">
          {copyStatus === 'success' ? '已复制' : null}
          {copyStatus === 'error' ? '复制失败，请手动选择文本复制' : null}
          {copyStatus === 'empty' ? '当前回答为空，无法复制' : null}
        </div>

        <div className="inline-flex items-center gap-1" aria-label="AI 回答操作">
          <button
            type="button"
            disabled={copyDisabled}
            onClick={() => void handleCopy()}
            className={actionButtonClass(false)}
            title={copyDisabled ? '当前回答为空，无法复制' : '复制回答'}
            aria-label={copyDisabled ? '当前回答为空，无法复制' : '复制回答'}
          >
            <CopyIcon className="h-[18px] w-[18px]" />
          </button>
          {showHelpful ? (
          <button
            type="button"
            disabled={Boolean(disabledReason) || submitting}
            onClick={() =>
              selectedType === 'HELPFUL'
                ? void cancel()
                : void submit('HELPFUL', 'ANSWER_ACCURATE')
            }
            className={actionButtonClass(selectedType === 'HELPFUL')}
            title={selectedType === 'HELPFUL' ? '撤回有帮助反馈' : '有帮助'}
            aria-label={selectedType === 'HELPFUL' ? '撤回有帮助反馈' : '有帮助'}
            aria-pressed={selectedType === 'HELPFUL'}
          >
            {submitting && pendingType === 'HELPFUL' ? (
              <SpinnerIcon className="h-[17px] w-[17px]" />
            ) : (
              <ThumbsUpIcon className="h-[18px] w-[18px]" />
            )}
          </button>
          ) : null}
          {showNotHelpful ? (
          <button
            type="button"
            disabled={Boolean(disabledReason) || submitting}
            onClick={() => (selectedType === 'NOT_HELPFUL' ? void cancel() : setDialogOpen(true))}
            className={actionButtonClass(selectedType === 'NOT_HELPFUL')}
            title={selectedType === 'NOT_HELPFUL' ? '撤回无帮助反馈' : '无帮助'}
            aria-label={selectedType === 'NOT_HELPFUL' ? '撤回无帮助反馈' : '无帮助'}
            aria-pressed={selectedType === 'NOT_HELPFUL'}
          >
            {submitting && pendingType === 'NOT_HELPFUL' ? (
              <SpinnerIcon className="h-[17px] w-[17px]" />
            ) : (
              <ThumbsDownIcon className="h-[18px] w-[18px]" />
            )}
          </button>
          ) : null}
          <button
            type="button"
            disabled={retryDisabled || retrying || submitting}
            onClick={() => void onRetry()}
            className={actionButtonClass(false)}
            title={retryTitle}
            aria-label={retryTitle}
          >
            {retrying ? <SpinnerIcon className="h-[17px] w-[17px]" /> : <RefreshIcon className="h-[18px] w-[18px]" />}
          </button>
        </div>
      </div>

      {disabledReason ? <div className="mt-1 text-right text-xs text-slate-400">{disabledReason}</div> : null}
      {retryDisabledReason ? <div className="mt-1 text-right text-xs text-slate-400">{retryDisabledReason}</div> : null}
      {feedback?.error ? <div className="mt-1 text-right text-xs text-red-600">{feedback.error}</div> : null}

      {selectedType === 'NOT_HELPFUL' ? (
        <div className="mt-1 text-right">
          <button
            type="button"
            disabled={Boolean(disabledReason) || submitting}
            onClick={() => setDialogOpen(true)}
            className="text-xs text-slate-400 transition hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            修改原因
          </button>
        </div>
      ) : null}

      <FeedbackPanel
        open={dialogOpen}
        submitting={submitting}
        error={feedback?.error}
        initialReason={initialReason}
        initialComment={initialComment}
        onCancel={() => setDialogOpen(false)}
        onSubmit={(reason, comment) => void submit('NOT_HELPFUL', reason || 'OTHER', comment)}
      />
    </div>
  )
}
