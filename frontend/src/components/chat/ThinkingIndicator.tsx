type ThinkingIndicatorProps = {
  text?: string
  className?: string
}

export function ThinkingIndicator({ text = '正在生成', className = '' }: ThinkingIndicatorProps) {
  return (
    <div className={`flex items-center gap-2 text-slate-400 ${className}`}>
      <span className="thinking-orbit" aria-hidden="true">
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <span
            key={i}
            className="thinking-dot"
            style={{
              transform: `rotate(${i * 60}deg) translateX(8px)`,
              animationDelay: `${i * 0.1}s`,
            }}
          />
        ))}
      </span>
      <span className="text-sm">{text}</span>
    </div>
  )
}
