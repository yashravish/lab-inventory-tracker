import { useEffect, useRef, useState } from 'react'
import type { ReagentStatus } from '../types'

export type StatusFilterValue = 'ALL' | ReagentStatus

interface Option {
  value: StatusFilterValue
  label: string
}

const OPTIONS: Option[] = [
  { value: 'ALL', label: 'All' },
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'EXPIRED', label: 'Expired' },
]

interface Props {
  value: StatusFilterValue
  onChange: (v: StatusFilterValue) => void
}

export function StatusFilter({ value, onChange }: Props) {
  const [open, setOpen] = useState(false)
  const [focusedIndex, setFocusedIndex] = useState<number>(() => {
    const idx = OPTIONS.findIndex((o) => o.value === value)
    return idx < 0 ? 0 : idx
  })
  const containerRef = useRef<HTMLDivElement | null>(null)
  const triggerRef = useRef<HTMLButtonElement | null>(null)
  const optionRefs = useRef<Array<HTMLDivElement | null>>([])

  const activeLabel = OPTIONS.find((o) => o.value === value)?.label ?? 'All'

  useEffect(() => {
    if (!open) return
    function onDocMouseDown(e: MouseEvent) {
      const node = containerRef.current
      if (node && !node.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocMouseDown)
    return () => document.removeEventListener('mousedown', onDocMouseDown)
  }, [open])

  useEffect(() => {
    if (!open) return
    const idx = OPTIONS.findIndex((o) => o.value === value)
    setFocusedIndex(idx < 0 ? 0 : idx)
  }, [open, value])

  useEffect(() => {
    if (!open) return
    optionRefs.current[focusedIndex]?.focus()
  }, [open, focusedIndex])

  function selectAt(idx: number) {
    const opt = OPTIONS[idx]
    if (!opt) return
    onChange(opt.value)
    setOpen(false)
    triggerRef.current?.focus()
  }

  function onTriggerKeyDown(e: React.KeyboardEvent<HTMLButtonElement>) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      setOpen(true)
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      setOpen((o) => !o)
    } else if (e.key === 'Escape' && open) {
      e.preventDefault()
      setOpen(false)
    }
  }

  function onOptionKeyDown(e: React.KeyboardEvent<HTMLDivElement>, idx: number) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setFocusedIndex((idx + 1) % OPTIONS.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setFocusedIndex((idx - 1 + OPTIONS.length) % OPTIONS.length)
    } else if (e.key === 'Home') {
      e.preventDefault()
      setFocusedIndex(0)
    } else if (e.key === 'End') {
      e.preventDefault()
      setFocusedIndex(OPTIONS.length - 1)
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      selectAt(idx)
    } else if (e.key === 'Escape') {
      e.preventDefault()
      setOpen(false)
      triggerRef.current?.focus()
    } else if (e.key === 'Tab') {
      setOpen(false)
    }
  }

  return (
    <div className="status-menu" ref={containerRef}>
      <button
        ref={triggerRef}
        type="button"
        className="status-menu-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label="Status filter"
        onClick={() => setOpen((o) => !o)}
        onKeyDown={onTriggerKeyDown}
      >
        <span className="status-menu-label">{activeLabel}</span>
        <span className="status-menu-caret" aria-hidden="true">▾</span>
      </button>
      {open && (
        <div className="status-menu-list" role="listbox">
          {OPTIONS.map((opt, idx) => {
            const selected = opt.value === value
            return (
              <div
                key={opt.value}
                ref={(el) => { optionRefs.current[idx] = el }}
                role="option"
                aria-selected={selected}
                tabIndex={-1}
                className={`status-menu-option${selected ? ' is-selected' : ''}`}
                onClick={() => selectAt(idx)}
                onKeyDown={(e) => onOptionKeyDown(e, idx)}
              >
                <span>{opt.label}</span>
                {selected && <span className="status-menu-check" aria-hidden="true">✓</span>}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
