import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api, ConflictError, UnauthorizedError } from './api'
import type { AiQueryResponse, AuthUser, DashboardSummary, Reagent, ReagentInput, ReagentStatus } from './types'
import { DashboardCards } from './components/DashboardCards'
import { SearchBar } from './components/SearchBar'
import { ReagentTable, type SortDir, type SortKey } from './components/ReagentTable'
import { ReagentForm } from './components/ReagentForm'
import { AskBar } from './components/AskBar'
import { LoginGate } from './components/LoginGate'

type Filter = 'ALL' | ReagentStatus

const SORTABLE_FIELDS: ReadonlyArray<SortKey> = [
  'name',
  'supplier',
  'quantity',
  'storageLocation',
  'expirationDate',
]

function initials(name: string): string {
  // Split on whitespace AND non-alphanumeric (so "yash.s" → ["yash", "s"] → "YS").
  const parts = name.trim().split(/[^A-Za-z0-9]+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

export default function App() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null)
  const [authChecked, setAuthChecked] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const userChipRef = useRef<HTMLDivElement | null>(null)

  const [reagents, setReagents] = useState<Reagent[]>([])
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<Filter>('ALL')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<Reagent | null>(null)
  const [sortKey, setSortKey] = useState<SortKey>('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(25)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [importNotice, setImportNotice] = useState<string | null>(null)
  const [aiResult, setAiResult] = useState<AiQueryResponse | null>(null)
  const [aiBusy, setAiBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    api.auth.me().then(
      (u) => { if (!cancelled) { setAuthUser(u); setAuthChecked(true) } },
      () => { if (!cancelled) { setAuthUser(null); setAuthChecked(true) } },
    )
    return () => { cancelled = true }
  }, [])

  const load = useCallback(async (opts: {
    search: string
    page: number
    size: number
    sortKey: SortKey
    sortDir: SortDir
  }) => {
    setLoading(true)
    try {
      const sortParam = SORTABLE_FIELDS.includes(opts.sortKey)
        ? `${opts.sortKey},${opts.sortDir}`
        : `name,asc`
      const [pageRes, sum] = await Promise.all([
        api.listReagents({
          search: opts.search,
          page: opts.page,
          size: opts.size,
          sort: sortParam,
        }),
        api.summary(),
      ])
      setReagents(pageRes.content)
      setTotalElements(pageRes.totalElements)
      setTotalPages(pageRes.totalPages)
      setSummary(sum)
      setError(null)
    } catch (e) {
      if (e instanceof UnauthorizedError) {
        setAuthUser(null)
        return
      }
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!authUser) return
    if (aiResult) return
    const handle = setTimeout(
      () => { void load({ search, page, size: pageSize, sortKey, sortDir }) },
      search ? 200 : 0,
    )
    return () => clearTimeout(handle)
  }, [authUser, search, page, pageSize, sortKey, sortDir, load, aiResult])

  // Status filter is applied client-side on the current page.
  // Server pagination is the source of truth; the filter narrows what's visible
  // among the rows already fetched. The pitch: server pagination is correct,
  // this is a pragmatic local refinement.
  const visibleReagents = useMemo(() => {
    if (aiResult) return aiResult.page.content
    return statusFilter === 'ALL'
      ? reagents
      : reagents.filter((r) => r.status === statusFilter)
  }, [reagents, statusFilter, aiResult])

  const displayTotalElements = aiResult ? aiResult.page.totalElements : totalElements
  const displayTotalPages = aiResult ? aiResult.page.totalPages : totalPages
  const displayPage = aiResult ? aiResult.page.number : page
  const displayPageSize = aiResult ? aiResult.page.size : pageSize

  function onSort(key: SortKey) {
    if (key === 'status') return // status is computed, not server-sortable
    if (key === sortKey) setSortDir(sortDir === 'asc' ? 'desc' : 'asc')
    else { setSortKey(key); setSortDir('asc') }
    setPage(0)
  }

  function onSearchChange(v: string) {
    setSearch(v)
    setPage(0)
  }

  function onStatusFilterChange(f: Filter) {
    setStatusFilter(f)
  }

  function onPageSizeChange(size: number) {
    setPageSize(size)
    setPage(0)
  }

  function openCreate() { setEditing(null); setDrawerOpen(true) }
  function openEdit(r: Reagent) { setEditing(r); setDrawerOpen(true) }
  function closeDrawer() { setDrawerOpen(false); setEditing(null) }

  const reload = useCallback(
    () => load({ search, page, size: pageSize, sortKey, sortDir }),
    [load, search, page, pageSize, sortKey, sortDir],
  )

  async function handleSubmit(input: ReagentInput) {
    try {
      if (editing) await api.updateReagent(editing.id, input)
      else await api.createReagent(input)
      closeDrawer()
      await reload()
    } catch (e) {
      if (e instanceof ConflictError) throw e
      setError(e instanceof Error ? e.message : 'Save failed')
    }
  }

  async function handleDelete(r: Reagent) {
    if (!window.confirm(`Delete ${r.name}?`)) return
    try {
      await api.deleteReagent(r.id)
      await reload()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Delete failed')
    }
  }

  async function handleImport(file: File) {
    try {
      const result = await api.importCsv(file)
      setImportNotice(
        `Imported: ${result.created} created, ${result.updated} updated, ${result.skipped} skipped.`,
      )
      setError(null)
      await reload()
    } catch (e) {
      setImportNotice(null)
      setError(e instanceof Error ? e.message : 'Import failed')
    }
  }

  useEffect(() => {
    if (!importNotice) return
    const t = setTimeout(() => setImportNotice(null), 4000)
    return () => clearTimeout(t)
  }, [importNotice])

  async function handleAsk(q: string) {
    setAiBusy(true)
    try {
      const res = await api.aiQuery(q)
      setAiResult(res)
      setError(null)
      const sum = await api.summary().catch(() => null)
      if (sum) setSummary(sum)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'AI query failed')
    } finally {
      setAiBusy(false)
    }
  }

  function clearAi() {
    setAiResult(null)
  }

  async function handleSignOut() {
    setUserMenuOpen(false)
    try {
      await api.auth.logout()
    } catch {
      // Even if the call fails, drop client-side state so the login gate returns.
    }
    setAuthUser(null)
    setReagents([])
    setSummary(null)
    setAiResult(null)
    setError(null)
  }

  useEffect(() => {
    if (!userMenuOpen) return
    function onDocClick(e: MouseEvent) {
      if (!userChipRef.current) return
      if (!userChipRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [userMenuOpen])

  if (!authChecked) {
    return null
  }

  if (!authUser) {
    return <LoginGate onAuthenticated={(u) => setAuthUser(u)} />
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <div className="topbar-left">
            <div className="app-title">Lab Inventory Tracker</div>
          </div>
          <div className="user-chip-wrap" ref={userChipRef}>
            <button
              type="button"
              className="user-chip-button"
              aria-haspopup="menu"
              aria-expanded={userMenuOpen}
              onClick={() => setUserMenuOpen((v) => !v)}
            >
              <div className="user-avatar">{initials(authUser.username)}</div>
              <span>{authUser.username}</span>
              <span className="status-menu-caret" aria-hidden="true">▾</span>
            </button>
            {userMenuOpen && (
              <div role="menu" className="status-menu-list user-menu-list">
                <div
                  role="menuitem"
                  tabIndex={0}
                  className="status-menu-option"
                  onClick={() => void handleSignOut()}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') void handleSignOut() }}
                >
                  Sign out
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="shell">
        <div className="page-head">
          <h1>Inventory</h1>
          <button className="btn btn-primary" onClick={openCreate}>Add</button>
        </div>

        <AskBar
          interpretation={aiResult?.interpretation ?? null}
          busy={aiBusy}
          onAsk={(q) => void handleAsk(q)}
          onClear={clearAi}
        />

        <DashboardCards summary={summary} filter={statusFilter} onFilterChange={onStatusFilterChange} />

        {aiResult && (
          <div className="ai-banner" role="status">
            AI filter applied — Clear to use manual filters.
          </div>
        )}

        {error && (
          <div className="error-banner" role="alert">
            <span>{error}</span>
            <button className="link-btn" onClick={() => void reload()}>Retry</button>
          </div>
        )}

        {importNotice && (
          <div className="success-banner" role="status">
            <span>{importNotice}</span>
            <button className="link-btn" onClick={() => setImportNotice(null)}>Dismiss</button>
          </div>
        )}

        <ReagentTable
          reagents={visibleReagents}
          loading={loading || aiBusy}
          search={search}
          sortKey={sortKey}
          sortDir={sortDir}
          onSort={onSort}
          onEdit={openEdit}
          onDelete={handleDelete}
          onClearSearch={() => onSearchChange('')}
          onCreate={openCreate}
          page={displayPage}
          pageSize={displayPageSize}
          totalElements={displayTotalElements}
          totalPages={displayTotalPages}
          statusFilterActive={statusFilter !== 'ALL' && !aiResult}
          onPageChange={setPage}
          onPageSizeChange={onPageSizeChange}
          toolbar={
            <SearchBar
              search={search}
              onSearchChange={onSearchChange}
              statusFilter={statusFilter}
              onStatusFilterChange={onStatusFilterChange}
              rowCount={displayTotalElements}
              exportHref={api.exportCsvUrl()}
              onImportFile={(f) => void handleImport(f)}
            />
          }
        />
      </main>

      <ReagentForm open={drawerOpen} editing={editing} onCancel={closeDrawer} onSubmit={handleSubmit} />
    </>
  )
}
