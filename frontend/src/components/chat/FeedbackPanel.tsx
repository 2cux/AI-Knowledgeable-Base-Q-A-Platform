import { useEffect, useState } from 'react'
import type { FeedbackReason } from '../../types/feedback'

export const FEEDBACK_COMMENT_LIMIT = 500

export const NOT_HELPFUL_REASONS: Array<{ value: FeedbackReason; label: string }> = [
  { value: 'ANSWER_INACCURATE', label: '答案不准确' },
  { value: 'SOURCE_NOT_RELEVANT', label: '引用来源不相关' },
  { value: 'ANSWER_INCOMPLETE', label: '回答不完整' },
  { value: 'HALLUCINATION', label: '存在幻觉' },
  { value: 'FORMAT_BAD', label: '格式不好' },
  { value: 'OTHER', label: '其他' },
]

type FeedbackPanelProps = {
  open: boolean
  submitting?: boolean
  error?: string
  initialReason?: FeedbackReason
  initialComment?: string
  onCancel: () => void
  onSubmit: (reason: FeedbackReason, comment: string) => void
}

export function FeedbackPanel({
  open,
  submitting,
  error,
  initialReason = 'ANSWER_INACCURATE',
  initialComment = '',
  onCancel,
  onSubmit,
}: FeedbackPanelProps) {
  const [reason, setReason] = useState<FeedbackReason>(initialReason)
  const [comment, setComment] = useState(initialComment.slice(0, FEEDBACK_COMMENT_LIMIT))
  const isOverLimit = comment.length > FEEDBACK_COMMENT_LIMIT

  useEffect(() => {
    if (open) {
      setReason(initialReason)
      setComment(initialComment.slice(0, FEEDBACK_COMMENT_LIMIT))
    }
  }, [initialComment, initialReason, open])

  if (!open) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) {
          onCancel()
        }
      }}
    >
      <div
        className="w-full max-w-[520px] rounded-md border border-slate-200 bg-white p-5 shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="feedback-dialog-title"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 id="feedback-dialog-title" className="text-base font-semibold text-slate-950">
              反馈这条回答
            </h2>
            <p className="mt-1 text-sm text-slate-500">请选择这条回答不满意的原因。</p>
          </div>
          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
            aria-label="关闭反馈弹窗"
            title="关闭"
          >
            ×
          </button>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          {NOT_HELPFUL_REASONS.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setReason(item.value)}
              disabled={submitting}
              className={[
                'rounded-md border px-3 py-1.5 text-sm transition disabled:cursor-not-allowed disabled:opacity-60',
                reason === item.value
                  ? 'border-slate-900 bg-slate-900 text-white'
                  : 'border-slate-200 bg-white text-slate-600 hover:border-slate-400 hover:text-slate-900',
              ].join(' ')}
            >
              {item.label}
            </button>
          ))}
        </div>

        <label className="mt-4 block">
          <span className="sr-only">补充说明</span>
          <textarea
            value={comment}
            onChange={(event) => setComment(event.target.value.slice(0, FEEDBACK_COMMENT_LIMIT))}
            disabled={submitting}
            maxLength={FEEDBACK_COMMENT_LIMIT}
            rows={4}
            placeholder="可选：补充说明，最多 500 字"
            className="w-full resize-none rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
          />
        </label>

        <div className="mt-2 flex items-center justify-between gap-3 text-xs">
          <span className={isOverLimit ? 'text-red-600' : 'text-slate-400'}>
            {comment.length}/{FEEDBACK_COMMENT_LIMIT}
          </span>
          {error ? <span className="text-red-600">{error}</span> : null}
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            className="rounded-md border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 transition hover:border-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
          >
            取消
          </button>
          <button
            type="button"
            onClick={() => onSubmit(reason || 'OTHER', comment)}
            disabled={submitting || isOverLimit}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {submitting ? '提交中...' : '提交反馈'}
          </button>
        </div>
      </div>
    </div>
  )
}
