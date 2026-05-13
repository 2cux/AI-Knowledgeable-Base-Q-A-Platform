import { useState } from 'react'
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
  submitting?: boolean
  error?: string
  onCancel: () => void
  onSubmit: (reason: FeedbackReason, comment: string) => void
}

export function FeedbackPanel({ submitting, error, onCancel, onSubmit }: FeedbackPanelProps) {
  const [reason, setReason] = useState<FeedbackReason>('OTHER')
  const [comment, setComment] = useState('')
  const maxCommentLength = FEEDBACK_COMMENT_LIMIT - `[${reason}] `.length
  const isOverLimit = comment.length > maxCommentLength

  return (
    <div className="mt-3 rounded-md border border-slate-200 bg-slate-50 p-3">
      <div className="text-xs font-medium text-slate-700">请选择无帮助原因</div>
      <div className="mt-2 flex flex-wrap gap-2">
        {NOT_HELPFUL_REASONS.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setReason(item.value)}
            disabled={submitting}
            className={[
              'rounded-md border px-2.5 py-1 text-xs transition',
              reason === item.value
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:border-slate-400',
            ].join(' ')}
          >
            {item.label}
          </button>
        ))}
      </div>

      <label className="mt-3 block">
        <span className="sr-only">补充说明</span>
        <textarea
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          disabled={submitting}
          maxLength={maxCommentLength + 50}
          rows={3}
          placeholder="可选：补充说明，最多 500 字"
          className="w-full resize-none rounded-md border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-slate-900 focus:ring-2 focus:ring-slate-200"
        />
      </label>

      <div className="mt-1 flex items-center justify-between gap-3 text-xs">
        <span className={isOverLimit ? 'text-red-600' : 'text-slate-400'}>
          {comment.length}/{maxCommentLength}
        </span>
        {error ? <span className="text-red-600">{error}</span> : null}
      </div>

      <div className="mt-3 flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600 hover:border-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
        >
          取消
        </button>
        <button
          type="button"
          onClick={() => onSubmit(reason || 'OTHER', comment)}
          disabled={submitting || isOverLimit}
          className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {submitting ? '提交中...' : '提交反馈'}
        </button>
      </div>
    </div>
  )
}
