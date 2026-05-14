import { useEffect, useRef, useState } from 'react'
import type { MouseEvent } from 'react'

import type { ConversationSummary } from '../../types/conversation'

type ConversationListItemProps = {
  conversation: ConversationSummary
  selected: boolean
  onSelect: (conversationId: string) => void
  onRename: (conversation: ConversationSummary) => void
  onDelete: (conversation: ConversationSummary) => void
  onTogglePin: (conversation: ConversationSummary) => void
}

type MenuPosition = {
  top: number
  left: number
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

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path strokeLinecap="round" strokeLinejoin="round" d="m15 4 5 5-4 1-4.5 4.5.5 4.5-2 2-2.5-5-5-2.5 2-2 4.5.5L13 8l2-4Z" />
    </svg>
  )
}

function PencilIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path strokeLinecap="round" strokeLinejoin="round" d="m4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="m14 7 3 3" />
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 7h16" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7l1-3h4l1 3" />
    </svg>
  )
}

function EllipsisIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4" fill="currentColor">
      <circle cx="5" cy="12" r="1.6" />
      <circle cx="12" cy="12" r="1.6" />
      <circle cx="19" cy="12" r="1.6" />
    </svg>
  )
}

export function ConversationListItem({
  conversation,
  selected,
  onSelect,
  onRename,
  onDelete,
  onTogglePin,
}: ConversationListItemProps) {
  const buttonRef = useRef<HTMLButtonElement | null>(null)
  const menuRef = useRef<HTMLDivElement | null>(null)
  const [menuPosition, setMenuPosition] = useState<MenuPosition | null>(null)
  const summary = conversation.lastQuestion || conversation.lastAnswerPreview || '暂无摘要'
  const menuOpen = menuPosition !== null
  const pinned = Boolean(conversation.pinned)

  useEffect(() => {
    if (!menuOpen) {
      return undefined
    }

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node | null
      if (
        target &&
        (buttonRef.current?.contains(target) || menuRef.current?.contains(target))
      ) {
        return
      }
      setMenuPosition(null)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMenuPosition(null)
      }
    }

    function handleScroll() {
      setMenuPosition(null)
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    window.addEventListener('scroll', handleScroll, true)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
      window.removeEventListener('scroll', handleScroll, true)
    }
  }, [menuOpen])

  function toggleMenu(event: MouseEvent<HTMLButtonElement>) {
    // Prevent the row click handler from loading conversation detail when the user opens actions.
    event.stopPropagation()
    const rect = event.currentTarget.getBoundingClientRect()
    const menuWidth = 144
    const left = Math.min(Math.max(8, rect.right - menuWidth), window.innerWidth - menuWidth - 8)
    setMenuPosition((current) => (current ? null : { top: rect.bottom + 6, left }))
  }

  function handleMenuAction(action: () => void) {
    setMenuPosition(null)
    action()
  }

  return (
    <div
      className={[
        'group relative rounded-md border transition',
        selected || menuOpen
          ? 'border-slate-900 bg-slate-900 text-white'
          : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50',
      ].join(' ')}
    >
      <button
        type="button"
        onClick={() => onSelect(conversation.conversationId)}
        className="block w-full rounded-md py-2.5 pl-3 pr-10 text-left"
      >
        <div className="flex items-start gap-3">
          <div className="flex min-w-0 flex-1 items-center gap-1.5">
            {pinned ? (
              <span
                title="已置顶"
                className={selected || menuOpen ? 'shrink-0 text-slate-200' : 'shrink-0 text-slate-500'}
              >
                <PinIcon />
              </span>
            ) : null}
            <span className="min-w-0 flex-1 truncate text-sm font-medium">
              {conversation.title || conversation.lastQuestion || '新会话'}
            </span>
          </div>
        </div>
        <div className={selected || menuOpen ? 'mt-1 truncate text-xs text-slate-200' : 'mt-1 truncate text-xs text-slate-500'}>
          {summary}
        </div>
        <div className={selected || menuOpen ? 'mt-1 text-xs text-slate-300' : 'mt-1 text-xs text-slate-400'}>
          {formatTime(conversation.lastActiveAt || conversation.createdAt)}
        </div>
      </button>

      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        aria-label={`更多操作：${conversation.title || '未命名会话'}`}
        onClick={toggleMenu}
        className={[
          'absolute right-2 top-2 inline-flex h-7 w-7 items-center justify-center rounded text-slate-400 transition hover:text-slate-900',
          selected || menuOpen
            ? 'bg-white/10 text-slate-200 hover:bg-white/20 hover:text-white'
            : 'bg-transparent opacity-70 hover:bg-slate-100 group-hover:opacity-100',
        ].join(' ')}
      >
        <EllipsisIcon />
      </button>

      {menuPosition ? (
        <div
          ref={menuRef}
          role="menu"
          className="fixed z-50 w-36 rounded-md border border-slate-200 bg-white py-1 text-sm text-slate-700 shadow-lg"
          style={{ top: menuPosition.top, left: menuPosition.left }}
          onClick={(event) => event.stopPropagation()}
        >
          <button
            type="button"
            role="menuitem"
            onClick={() => handleMenuAction(() => onTogglePin(conversation))}
            className="flex w-full items-center gap-2 px-3 py-2 text-left hover:bg-slate-50 hover:text-slate-950"
          >
            <PinIcon />
            <span>{pinned ? '取消置顶' : '置顶'}</span>
          </button>
          <button
            type="button"
            role="menuitem"
            onClick={() => handleMenuAction(() => onRename(conversation))}
            className="flex w-full items-center gap-2 px-3 py-2 text-left hover:bg-slate-50 hover:text-slate-950"
          >
            <PencilIcon />
            <span>重命名</span>
          </button>
          <button
            type="button"
            role="menuitem"
            onClick={() => handleMenuAction(() => onDelete(conversation))}
            className="flex w-full items-center gap-2 px-3 py-2 text-left text-red-600 hover:bg-red-50"
          >
            <TrashIcon />
            <span>删除</span>
          </button>
        </div>
      ) : null}
    </div>
  )
}
