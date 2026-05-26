import { useState } from 'react'

interface Props {
  interpretation: string | null
  busy: boolean
  onAsk: (q: string) => void
  onClear: () => void
}

export function AskBar({ interpretation, busy, onAsk, onClear }: Props) {
  const [value, setValue] = useState('')

  function submit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = value.trim()
    if (!trimmed || busy) return
    onAsk(trimmed)
  }

  function clear() {
    setValue('')
    onClear()
  }

  return (
    <div className="askbar" data-testid="askbar">
      <form className="askbar-input-wrap" onSubmit={submit} role="search">
        <input
          type="text"
          className="askbar-input"
          placeholder="Ask: 'flammables in Lab-2 expiring within 30 days'"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          aria-label="Ask your inventory"
          disabled={busy}
        />
        <span className="askbar-chip" aria-hidden="true">Ask AI</span>
      </form>
      {interpretation && (
        <div className="askbar-interp" data-testid="askbar-interp">
          <span>Showing: {interpretation}</span>
          <button type="button" className="link-btn" onClick={clear} aria-label="Clear AI filter">
            Clear
          </button>
        </div>
      )}
    </div>
  )
}
