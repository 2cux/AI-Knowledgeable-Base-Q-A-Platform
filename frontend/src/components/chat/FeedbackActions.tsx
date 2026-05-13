import { useState } from 'react'
import { splitStoredFeedbackComment } from '../../api/feedback'
import type {
  FeedbackReason,
  FeedbackState,
  FeedbackSubmitRequest,
  FeedbackType,
} from '../../types/feedback'
import { FeedbackPanel } from './FeedbackPanel'

type FeedbackActionsProps = {
  knowledgeBaseId?: number | string
  conversationId?: string
  messageId?: string
  chatRecordId?: number | string
  feedback?: FeedbackState
  onSubmit: (request: FeedbackSubmitRequest) => Promise<void>
}

const FEEDBACK_LABELS: Record<FeedbackType, string> = {
  HELPFUL: '有帮助',
  NOT_HELPFUL: '无帮助',
}

const REASON_LABELS: Record<FeedbackReason, string> = {
  ANSWER_ACCURATE: '回答准确',
  ANSWER_INACCURATE: '答案不准确',
  SOURCE_NOT_RELEVANT: '引用来源不相关',
  ANSWER_INCOMPLETE: '回答不完整',
  HALLUCINATION: '存在幻觉',
  FORMAT_BAD: '格式不好',
  OTHER: '其他',
}

function unavailableReason({
  knowledgeBaseId,
  conversationId,
  chatRecordId,
}: Pick<FeedbackActionsProps, 'knowledgeBaseId' | 'conversationId' | 'chatRecordId'>) {
  if (!knowledgeBaseId) {
    return '缺少知识库 ID，暂时无法反馈。'
  }

  if (!conversationId) {
    return '缺少会话 ID，暂时无法反馈。'
  }

  if (!chatRecordId) {
    return '缺少问答记录 ID，暂时无法反馈。'
  }

  return ''
}

export function FeedbackActions({
  knowledgeBaseId,
  conversationId,
  messageId,
  chatRecordId,
  feedback,
  onSubmit,
}: FeedbackActionsProps) {
  const [panelOpen, setPanelOpen] = useState(false)
  const disabledReason = unavailableReason({ knowledgeBaseId, conversationId, chatRecordId })
  const submitted = Boolean(feedback?.submitted || feedback?.feedbackType)
  const submitting = Boolean(feedback?.submitting)
  const stored = splitStoredFeedbackComment(feedback?.comment)
  const selectedType = feedback?.feedbackType
  const selectedReason = feedback?.reason ?? stored.reason
  const selectedComment = feedback?.comment && stored.reason ? stored.comment : feedback?.comment

  async function submit(feedbackType: FeedbackType, reason: FeedbackReason, comment?: string) {
    if (disabledReason || submitted || submitting) {
      return
    }

    try {
      await onSubmit({
        knowledgeBaseId: knowledgeBaseId as number | string,
        conversationId,
        messageId,
        chatRecordId,
        feedbackType,
        reason,
        comment,
      })
      setPanelOpen(false)
    } catch {
      // The parent stores the inline error on the message so user input remains retryable.
    }
  }

  return (
    <div className="border-t border-slate-100 pt-3">
      <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span>这条回答是否有帮助？</span>
        <button
          type="button"
          disabled={Boolean(disabledReason) || submitted || submitting}
          onClick={() => void submit('HELPFUL', 'ANSWER_ACCURATE')}
          className={[
            'rounded-md border px-2.5 py-1 transition disabled:cursor-not-allowed disabled:opacity-60',
            selectedType === 'HELPFUL'
              ? 'border-emerald-600 bg-emerald-50 text-emerald-700'
              : 'border-slate-200 bg-white text-slate-600 hover:border-slate-400',
          ].join(' ')}
        >
          {submitting && selectedType === 'HELPFUL' ? '提交中...' : '有帮助'}
        </button>
        <button
          type="button"
          disabled={Boolean(disabledReason) || submitted || submitting}
          onClick={() => setPanelOpen(true)}
          className={[
            'rounded-md border px-2.5 py-1 transition disabled:cursor-not-allowed disabled:opacity-60',
            selectedType === 'NOT_HELPFUL'
              ? 'border-amber-600 bg-amber-50 text-amber-700'
              : 'border-slate-200 bg-white text-slate-600 hover:border-slate-400',
          ].join(' ')}
        >
          {submitting && selectedType === 'NOT_HELPFUL' ? '提交中...' : '无帮助'}
        </button>

        {submitted && selectedType ? (
          <span className="text-emerald-700">
            已记录反馈：{FEEDBACK_LABELS[selectedType]}
            {selectedReason ? ` · ${REASON_LABELS[selectedReason]}` : ''}
          </span>
        ) : null}
      </div>

      {disabledReason ? <div className="mt-2 text-xs text-slate-400">{disabledReason}</div> : null}
      {feedback?.error ? <div className="mt-2 text-xs text-red-600">{feedback.error}</div> : null}
      {submitted && selectedComment ? (
        <div className="mt-2 rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
          补充说明：{selectedComment}
        </div>
      ) : null}

      {panelOpen && !submitted ? (
        <FeedbackPanel
          submitting={submitting}
          error={feedback?.error}
          onCancel={() => setPanelOpen(false)}
          onSubmit={(reason, comment) => void submit('NOT_HELPFUL', reason || 'OTHER', comment)}
        />
      ) : null}
    </div>
  )
}
