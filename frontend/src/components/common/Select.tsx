import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'

export type SelectValue = string | number

export type SelectOption = {
  label: string
  value: SelectValue | ''
  disabled?: boolean
}

type SelectProps = {
  value: SelectValue | ''
  options: SelectOption[]
  onChange: (value: SelectValue | '') => void
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  className?: string
  size?: 'normal' | 'sm'
  ariaLabel?: string
}

function cx(...classes: Array<string | false | null | undefined>) {
  return classes.filter(Boolean).join(' ')
}

function sameValue(left: SelectValue | '', right: SelectValue | '') {
  return String(left) === String(right)
}

export function Select({
  value,
  options,
  onChange,
  placeholder = '请选择',
  disabled = false,
  loading = false,
  className,
  size = 'normal',
  ariaLabel,
}: SelectProps) {
  const selectId = useId()
  const rootRef = useRef<HTMLDivElement>(null)
  const [open, setOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)

  const enabledOptions = useMemo(() => options.filter((option) => !option.disabled), [options])
  const selectedOption = value === '' ? undefined : options.find((option) => sameValue(value, option.value))
  const displayLabel = selectedOption?.label ?? ''
  const isDisabled = disabled || loading
  const hasOptions = options.length > 0

  useEffect(() => {
    if (!open) return

    function handlePointerDown(event: MouseEvent | TouchEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('touchstart', handlePointerDown)
    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('touchstart', handlePointerDown)
    }
  }, [open])

  useEffect(() => {
    if (isDisabled) setOpen(false)
  }, [isDisabled])

  useEffect(() => {
    if (!open) return

    const selectedIndex = options.findIndex((option) => sameValue(value, option.value) && !option.disabled)
    if (selectedIndex >= 0) {
      setActiveIndex(selectedIndex)
      return
    }

    const firstEnabled = options.findIndex((option) => !option.disabled)
    setActiveIndex(firstEnabled)
  }, [open, options, value])

  function selectOption(option: SelectOption) {
    if (option.disabled) return
    onChange(option.value)
    setOpen(false)
  }

  function moveActive(direction: 1 | -1) {
    if (enabledOptions.length === 0) return

    const currentValue = activeIndex >= 0 ? options[activeIndex]?.value : undefined
    const enabledIndex = enabledOptions.findIndex((option) => currentValue !== undefined && sameValue(currentValue, option.value))
    const nextEnabledIndex = enabledIndex < 0
      ? direction > 0 ? 0 : enabledOptions.length - 1
      : (enabledIndex + direction + enabledOptions.length) % enabledOptions.length
    const nextOption = enabledOptions[nextEnabledIndex]
    setActiveIndex(options.findIndex((option) => sameValue(option.value, nextOption.value)))
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (isDisabled) return

    if (event.key === 'Escape') {
      setOpen(false)
      return
    }

    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!open) {
        setOpen(true)
      } else {
        moveActive(event.key === 'ArrowDown' ? 1 : -1)
      }
      return
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (!open) {
        setOpen(true)
        return
      }

      const option = options[activeIndex]
      if (option) selectOption(option)
    }
  }

  return (
    <div ref={rootRef} className={cx('relative min-w-0', className)}>
      <button
        type="button"
        aria-label={ariaLabel ?? placeholder}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={`${selectId}-listbox`}
        disabled={isDisabled}
        onClick={() => {
          if (!isDisabled) setOpen((current) => !current)
        }}
        onKeyDown={handleKeyDown}
        className={cx(
          'flex w-full items-center justify-between gap-2 rounded-lg border border-slate-300 bg-white px-3 text-left text-sm text-slate-900 shadow-sm outline-none transition duration-150',
          'hover:border-slate-400 focus:border-slate-500 focus:ring-2 focus:ring-slate-200',
          'disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400 disabled:opacity-70',
          size === 'sm' ? 'h-9' : 'h-10',
        )}
      >
        <span className={cx('min-w-0 flex-1 truncate', displayLabel ? 'text-slate-900' : 'text-slate-400')}>
          {loading ? '加载中...' : displayLabel || placeholder}
        </span>
        <svg
          aria-hidden="true"
          viewBox="0 0 20 20"
          fill="none"
          className={cx('h-4 w-4 shrink-0 text-slate-500 transition-transform duration-150', open && 'rotate-180')}
        >
          <path d="M5 7.5 10 12.5 15 7.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open ? (
        <div
          id={`${selectId}-listbox`}
          role="listbox"
          className="app-select-dropdown-in absolute left-0 right-0 top-full z-50 mt-1 max-h-60 overflow-y-auto rounded-lg border border-slate-200 bg-white p-1 text-sm shadow-lg ring-1 ring-slate-900/5"
        >
          {!hasOptions ? (
            <div className="px-3 py-2 text-slate-400">暂无可选项</div>
          ) : (
            options.map((option, index) => {
              const selected = sameValue(value, option.value)
              const active = index === activeIndex
              return (
                <button
                  key={`${option.value}`}
                  type="button"
                  role="option"
                  aria-selected={selected}
                  disabled={option.disabled}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => selectOption(option)}
                  className={cx(
                    'flex w-full items-center justify-between gap-2 rounded-md px-3 py-2 text-left transition-colors duration-100',
                    'disabled:cursor-not-allowed disabled:text-slate-300',
                    selected ? 'bg-slate-100 text-slate-950' : 'text-slate-700',
                    active && !option.disabled && !selected && 'bg-slate-50 text-slate-950',
                  )}
                >
                  <span className="min-w-0 flex-1 truncate">{option.label}</span>
                  {selected ? <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-slate-500" /> : null}
                </button>
              )
            })
          )}
        </div>
      ) : null}
    </div>
  )
}
