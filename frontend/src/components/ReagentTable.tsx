import type { HazardClass, Reagent } from '../types'
import { StatusPill } from './StatusPill'

const HAZARD_LABEL: Record<Exclude<HazardClass, 'NONE'>, string> = {
  FLAMMABLE: 'FLAM',
  CORROSIVE: 'CORR',
  TOXIC: 'TOX',
}

type SortKey = 'name' | 'supplier' | 'quantity' | 'storageLocation' | 'expirationDate' | 'status'
type SortDir = 'asc' | 'desc'

const SERVER_SORTABLE: ReadonlyArray<SortKey> = [
  'name',
  'supplier',
  'quantity',
  'storageLocation',
  'expirationDate',
]

interface Props {
  reagents: Reagent[]
  loading: boolean
  search: string
  sortKey: SortKey
  sortDir: SortDir
  onSort: (key: SortKey) => void
  onEdit: (r: Reagent) => void
  onDelete: (r: Reagent) => void
  onClearSearch: () => void
  onCreate: () => void
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
  statusFilterActive: boolean
  onPageChange: (page: number) => void
  onPageSizeChange: (size: number) => void
  toolbar?: React.ReactNode
}

function formatQty(q: number | string): string {
  const n = typeof q === 'string' ? Number(q) : q
  if (Number.isNaN(n)) return String(q)
  return n.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 4 })
}

function expirationMeta(dateStr: string | null): { text: string; meta: string | null; metaClass: string | null } {
  if (!dateStr) return { text: '—', meta: null, metaClass: null }
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const exp = new Date(dateStr + 'T00:00:00')
  const diffMs = exp.getTime() - today.getTime()
  const diffDays = Math.round(diffMs / 86400000)
  if (diffDays < 0) return { text: dateStr, meta: '· expired', metaClass: 'err' }
  if (diffDays <= 30) return { text: dateStr, meta: `· ${diffDays}d`, metaClass: 'warn' }
  return { text: dateStr, meta: null, metaClass: null }
}

function SortCaret({ active, dir }: { active: boolean; dir: SortDir }) {
  if (!active) return <span className="caret">▾</span>
  return <span className="caret">{dir === 'asc' ? '▲' : '▼'}</span>
}

export function ReagentTable({
  reagents,
  loading,
  search,
  sortKey,
  sortDir,
  onSort,
  onEdit,
  onDelete,
  onClearSearch,
  onCreate,
  page,
  pageSize,
  totalElements,
  totalPages,
  statusFilterActive,
  onPageChange,
  onPageSizeChange,
  toolbar,
}: Props) {
  const sortable = (key: SortKey, label: string, align: 'left' | 'right' = 'left') => {
    const enabled = SERVER_SORTABLE.includes(key)
    return (
      <th
        className={enabled ? 'sortable' : undefined}
        style={{ textAlign: align }}
        onClick={enabled ? () => onSort(key) : undefined}
        aria-sort={sortKey === key ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'}
      >
        {label}
        {enabled && <SortCaret active={sortKey === key} dir={sortDir} />}
      </th>
    )
  }

  const lastPage = Math.max(0, totalPages - 1)
  const onFirstPage = page <= 0
  const onLastPage = totalPages === 0 || page >= lastPage
  const pageDisplay = totalPages === 0 ? 0 : page + 1
  const pageCountDisplay = Math.max(1, totalPages)

  return (
    <div className="surface">
      {loading && <div className="loading-bar" data-testid="loading-bar" />}
      {toolbar}
      <table className="reagents" data-testid="reagent-table">
        <thead>
          <tr>
            {sortable('name', 'Name')}
            {sortable('supplier', 'Supplier')}
            {sortable('quantity', 'Quantity', 'right')}
            {sortable('storageLocation', 'Storage')}
            {sortable('expirationDate', 'Expiration')}
            {sortable('status', 'Status')}
            <th style={{ textAlign: 'right' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading && reagents.length === 0
            ? Array.from({ length: 6 }).map((_, i) => (
                <tr key={`sk-${i}`} className="skeleton-row">
                  <td colSpan={7}><div className="skeleton-block" /></td>
                </tr>
              ))
            : reagents.length === 0
            ? (
                <tr>
                  <td colSpan={7}>
                    <div className="empty">
                      {search ? (
                        <>
                          <div className="empty-title">No reagents match &ldquo;{search}&rdquo;.</div>
                          <button className="link-btn" onClick={onClearSearch}>Clear search</button>
                        </>
                      ) : (
                        <>
                          <div className="empty-title">No reagents yet.</div>
                          <div className="empty-sub">Add one to get started.</div>
                          <button className="btn btn-ghost" onClick={onCreate}>Add</button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              )
            : reagents.map((r) => {
                const exp = expirationMeta(r.expirationDate)
                return (
                  <tr key={r.id} className={r.status === 'EXPIRED' ? 'row-expired' : undefined}>
                    <td className="col-name">
                      {r.name}
                      {r.hazardClass && r.hazardClass !== 'NONE' && (
                        <span className={`haz haz-${r.hazardClass.toLowerCase()}`}>
                          {HAZARD_LABEL[r.hazardClass]}
                        </span>
                      )}
                      {r.casNumber && <span className="cas">CAS {r.casNumber}</span>}
                    </td>
                    <td className="col-supplier">{r.supplier ?? '—'}</td>
                    <td className="col-qty">
                      {formatQty(r.quantity)}
                      {r.unit && <span className="unit">{r.unit}</span>}
                    </td>
                    <td className="col-loc">{r.storageLocation ?? '—'}</td>
                    <td className="col-exp">
                      {exp.text}
                      {exp.meta && <span className={`meta ${exp.metaClass}`}>{exp.meta}</span>}
                    </td>
                    <td><StatusPill status={r.status} /></td>
                    <td className="col-actions">
                      <button className="btn-edit-link" onClick={() => onEdit(r)} aria-label={`Edit ${r.name}`}>Edit</button>
                      <span className="sep">·</span>
                      <button className="btn-danger-link" onClick={() => onDelete(r)} aria-label={`Delete ${r.name}`}>Delete</button>
                    </td>
                  </tr>
                )
              })}
        </tbody>
      </table>
      <div className="pagination" data-testid="pagination">
        <div className="pagination-left">
          <label className="page-size-label">
            Show
            <select
              className="page-size-select"
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              aria-label="Rows per page"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
            per page
          </label>
          <span className="pagination-status">
            Page {pageDisplay} of {pageCountDisplay} · {totalElements} {totalElements === 1 ? 'row' : 'rows'}
          </span>
          {statusFilterActive && (
            <span className="pagination-hint">Status filter applies to this page only</span>
          )}
        </div>
        <div className="pagination-right">
          <button
            className="btn btn-ghost"
            onClick={() => onPageChange(Math.max(0, page - 1))}
            disabled={onFirstPage}
            aria-label="Previous page"
          >
            Prev
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => onPageChange(Math.min(lastPage, page + 1))}
            disabled={onLastPage}
            aria-label="Next page"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  )
}

export type { SortKey, SortDir }
