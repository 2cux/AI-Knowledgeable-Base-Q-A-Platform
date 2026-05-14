import type { KeyboardEvent } from 'react'

type ChatInputProps = {
  value: string
  disabled: boolean
  sending: boolean
  onChange: (value: string) => void
  onSend: () => void
}

export function ChatInput({ value, disabled, sending, onChange, onSend }: ChatInputProps) {
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      onSend()
    }
  }

  return (
    <div className="flex items-end gap-2">
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={2}
        maxLength={2000}
        placeholder="输入问题，Enter 发送，Shift + Enter 换行"
        className="min-h-[44px] max-h-32 flex-1 resize-none rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm leading-6 outline-none transition focus:border-slate-900 focus:ring-2 focus:ring-slate-200 disabled:cursor-not-allowed disabled:bg-slate-100"
      />
      <button
        type="button"
        onClick={onSend}
        disabled={disabled || !value.trim()}
        className="h-10 shrink-0 rounded-md bg-slate-900 px-4 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
      >
        {sending ? '发送中' : '发送'}
      </button>
    </div>
  )
}
