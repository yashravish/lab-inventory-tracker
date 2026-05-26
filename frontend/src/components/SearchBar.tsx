import { useRef } from 'react'
import type { ReagentStatus } from '../types'
import { StatusFilter } from './StatusFilter'

type Filter = 'ALL' | ReagentStatus

interface Props {
  search: string
  onSearchChange: (v: string) => void
  statusFilter: Filter
  onStatusFilterChange: (f: Filter) => void
  rowCount: number
  exportHref: string
  onImportFile: (file: File) => void
}

export function SearchBar({
  search,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  rowCount,
  exportHref,
  onImportFile,
}: Props) {
  const fileRef = useRef<HTMLInputElement>(null)
  return (
    <div className="toolbar">
      <div className="search">
        <svg
          className="search-icon"
          viewBox="0 0 16 16"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <circle cx="7" cy="7" r="5" />
          <path d="M11 11l3 3" />
        </svg>
        <input
          type="text"
          placeholder="Search name, supplier, location…"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          aria-label="Search reagents"
        />
      </div>
      <StatusFilter value={statusFilter} onChange={onStatusFilterChange} />
      <div className="toolbar-actions">
        <a className="btn btn-ghost" href={exportHref} download>Export CSV</a>
        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => fileRef.current?.click()}
        >
          Import…
        </button>
        <input
          ref={fileRef}
          type="file"
          accept=".csv,text/csv"
          aria-label="Import CSV file"
          style={{ display: 'none' }}
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) onImportFile(f)
            e.target.value = ''
          }}
        />
      </div>
      <div className="row-count">{rowCount} {rowCount === 1 ? 'reagent' : 'reagents'}</div>
    </div>
  )
}
