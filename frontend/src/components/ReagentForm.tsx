import { useEffect, useState } from 'react'
import { api, ConflictError } from '../api'
import type { HazardClass, Reagent, ReagentEvent, ReagentInput } from '../types'

interface Props {
  open: boolean
  editing: Reagent | null
  onCancel: () => void
  onSubmit: (input: ReagentInput) => Promise<void>
}

interface FormState {
  name: string
  supplier: string
  lotNumber: string
  casNumber: string
  hazardClass: HazardClass
  quantity: string
  unit: string
  storageLocation: string
  expirationDate: string
  minimumQuantity: string
}

const EMPTY: FormState = {
  name: '',
  supplier: '',
  lotNumber: '',
  casNumber: '',
  hazardClass: 'NONE',
  quantity: '',
  unit: 'g',
  storageLocation: '',
  expirationDate: '',
  minimumQuantity: '5',
}

const UNITS = ['g', 'mg', 'kg', 'mL', 'L', 'units']
const HAZARD_CLASSES: HazardClass[] = ['NONE', 'FLAMMABLE', 'CORROSIVE', 'TOXIC']

type Tab = 'details' | 'history'

function stateFromReagent(r: Reagent): FormState {
  return {
    name: r.name ?? '',
    supplier: r.supplier ?? '',
    lotNumber: r.lotNumber ?? '',
    casNumber: r.casNumber ?? '',
    hazardClass: r.hazardClass ?? 'NONE',
    quantity: String(r.quantity ?? ''),
    unit: r.unit ?? 'g',
    storageLocation: r.storageLocation ?? '',
    expirationDate: r.expirationDate ?? '',
    minimumQuantity: String(r.minimumQuantity ?? '5'),
  }
}

function formatTimestamp(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function eventPillClass(type: ReagentEvent['eventType']): string {
  switch (type) {
    case 'CREATED': return 'pill pill-ok'
    case 'DELETED': return 'pill pill-err'
    default: return 'pill pill-neutral'
  }
}

export function ReagentForm({ open, editing, onCancel, onSubmit }: Props) {
  const [state, setState] = useState<FormState>(EMPTY)
  const [submitting, setSubmitting] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [version, setVersion] = useState<number | undefined>(undefined)
  const [conflict, setConflict] = useState(false)
  const [reloading, setReloading] = useState(false)
  const [tab, setTab] = useState<Tab>('details')
  const [events, setEvents] = useState<ReagentEvent[] | null>(null)
  const [eventsLoading, setEventsLoading] = useState(false)
  const [eventsError, setEventsError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    if (editing) {
      setState(stateFromReagent(editing))
      setVersion(editing.version)
    } else {
      setState(EMPTY)
      setVersion(undefined)
    }
    setErrors({})
    setConflict(false)
    setTab('details')
    setEvents(null)
    setEventsError(null)
  }, [open, editing])

  useEffect(() => {
    if (!open || tab !== 'history' || !editing) return
    let cancelled = false
    setEventsLoading(true)
    setEventsError(null)
    api.listEvents(editing.id)
      .then((list) => { if (!cancelled) setEvents(list) })
      .catch((e: unknown) => {
        if (!cancelled) setEventsError(e instanceof Error ? e.message : 'Failed to load history')
      })
      .finally(() => { if (!cancelled) setEventsLoading(false) })
    return () => { cancelled = true }
  }, [open, tab, editing])

  if (!open) return null

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setState((s) => ({ ...s, [key]: value }))
  }

  function validate(): boolean {
    const e: Record<string, string> = {}
    if (!state.name.trim()) e.name = 'Name is required'
    const q = Number(state.quantity)
    if (state.quantity === '' || Number.isNaN(q)) e.quantity = 'Quantity is required'
    else if (q < 0) e.quantity = 'Must be ≥ 0'
    const m = Number(state.minimumQuantity)
    if (state.minimumQuantity !== '' && (Number.isNaN(m) || m < 0)) {
      e.minimumQuantity = 'Must be ≥ 0'
    }
    setErrors(e)
    return Object.keys(e).length === 0
  }

  async function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault()
    if (!validate()) return
    setSubmitting(true)
    try {
      const input: ReagentInput = {
        name: state.name.trim(),
        supplier: state.supplier.trim() || undefined,
        lotNumber: state.lotNumber.trim() || undefined,
        casNumber: state.casNumber.trim() || undefined,
        hazardClass: state.hazardClass,
        quantity: Number(state.quantity),
        unit: state.unit || undefined,
        storageLocation: state.storageLocation.trim() || undefined,
        expirationDate: state.expirationDate || undefined,
        minimumQuantity: state.minimumQuantity === '' ? undefined : Number(state.minimumQuantity),
        version: editing ? version : undefined,
      }
      try {
        await onSubmit(input)
      } catch (e) {
        if (e instanceof ConflictError) {
          setConflict(true)
        } else {
          throw e
        }
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReload() {
    if (!editing) return
    setReloading(true)
    try {
      const fresh = await api.getReagent(editing.id)
      setState(stateFromReagent(fresh))
      setVersion(fresh.version)
      setConflict(false)
      setErrors({})
    } catch {
      // leave banner visible; user can retry
    } finally {
      setReloading(false)
    }
  }

  return (
    <>
      <div className="overlay" onClick={onCancel} />
      <aside className="drawer" role="dialog" aria-label={editing ? 'Edit Reagent' : 'New Reagent'}>
        <div className="drawer-head">
          <span>{editing ? `Edit Reagent — ${editing.name}` : 'New Reagent'}</span>
          <button className="drawer-close" onClick={onCancel} aria-label="Close">×</button>
        </div>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
          <div className="drawer-body">
            {editing && (
              <div className="tab-strip" role="tablist">
                <button
                  type="button"
                  role="tab"
                  aria-selected={tab === 'details'}
                  className={`tab ${tab === 'details' ? 'tab-active' : ''}`}
                  onClick={() => setTab('details')}
                >
                  Details
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={tab === 'history'}
                  className={`tab ${tab === 'history' ? 'tab-active' : ''}`}
                  onClick={() => setTab('history')}
                >
                  History
                </button>
              </div>
            )}

            {tab === 'details' && (
              <>
                {conflict && (
                  <div className="error-banner" role="alert">
                    <span>This reagent was edited by someone else.</span>
                    <button
                      type="button"
                      className="link-btn"
                      onClick={() => void handleReload()}
                      disabled={reloading}
                    >
                      {reloading ? 'Reloading…' : 'Reload'}
                    </button>
                  </div>
                )}
                <div className="field">
                  <label htmlFor="name">Name<span className="req">*</span></label>
                  <input id="name" value={state.name} onChange={(e) => set('name', e.target.value)} />
                  {errors.name && <div className="err">{errors.name}</div>}
                </div>
                <div className="field">
                  <label htmlFor="supplier">Supplier</label>
                  <input id="supplier" value={state.supplier} onChange={(e) => set('supplier', e.target.value)} />
                </div>
                <div className="field-row-2">
                  <div className="field">
                    <label htmlFor="lotNumber">Lot number</label>
                    <input
                      id="lotNumber"
                      value={state.lotNumber}
                      onChange={(e) => set('lotNumber', e.target.value)}
                      placeholder="LOT-2024-A042"
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="casNumber">CAS number</label>
                    <input
                      id="casNumber"
                      value={state.casNumber}
                      onChange={(e) => set('casNumber', e.target.value)}
                      placeholder="64-17-5"
                    />
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="hazardClass">Hazard class</label>
                  <select
                    id="hazardClass"
                    value={state.hazardClass}
                    onChange={(e) => set('hazardClass', e.target.value as HazardClass)}
                  >
                    {HAZARD_CLASSES.map((h) => (
                      <option key={h} value={h}>{h.charAt(0) + h.slice(1).toLowerCase()}</option>
                    ))}
                  </select>
                </div>
                <div className="field-row-2">
                  <div className="field">
                    <label htmlFor="quantity">Quantity<span className="req">*</span></label>
                    <div className="field-row">
                      <input
                        id="quantity"
                        type="number"
                        step="any"
                        min="0"
                        value={state.quantity}
                        onChange={(e) => set('quantity', e.target.value)}
                      />
                      <select aria-label="Unit" value={state.unit} onChange={(e) => set('unit', e.target.value)}>
                        {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
                      </select>
                    </div>
                    {errors.quantity && <div className="err">{errors.quantity}</div>}
                  </div>
                  <div className="field">
                    <label htmlFor="minimumQuantity">Minimum quantity</label>
                    <input
                      id="minimumQuantity"
                      type="number"
                      step="any"
                      min="0"
                      value={state.minimumQuantity}
                      onChange={(e) => set('minimumQuantity', e.target.value)}
                    />
                    {errors.minimumQuantity && <div className="err">{errors.minimumQuantity}</div>}
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="storageLocation">Storage location</label>
                  <input
                    id="storageLocation"
                    value={state.storageLocation}
                    onChange={(e) => set('storageLocation', e.target.value)}
                    placeholder="Lab-2 / Shelf-B / Cab-3"
                  />
                </div>
                <div className="field">
                  <label htmlFor="expirationDate">Expiration date</label>
                  <input
                    id="expirationDate"
                    type="date"
                    value={state.expirationDate}
                    onChange={(e) => set('expirationDate', e.target.value)}
                  />
                </div>
              </>
            )}

            {tab === 'history' && editing && (
              <HistoryPanel
                events={events}
                loading={eventsLoading}
                error={eventsError}
              />
            )}
          </div>
          <div className="drawer-foot">
            {tab === 'details' && (
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving…' : 'Save Reagent'}
              </button>
            )}
          </div>
        </form>
      </aside>
    </>
  )
}

function HistoryPanel({
  events,
  loading,
  error,
}: {
  events: ReagentEvent[] | null
  loading: boolean
  error: string | null
}) {
  if (loading) {
    return <div className="history-empty">Loading history…</div>
  }
  if (error) {
    return (
      <div className="error-banner" role="alert">
        <span>{error}</span>
      </div>
    )
  }
  if (!events || events.length === 0) {
    return <div className="history-empty">No history yet.</div>
  }
  return (
    <ul className="event-list" data-testid="event-list">
      {events.map((e) => {
        const changeCount = e.changes ? Object.keys(e.changes).length : 0
        return (
          <li key={e.id} className="event-item" data-testid="event-row">
            <div className="event-row">
              <span className="event-time">{formatTimestamp(e.createdAt)}</span>
              <span className={eventPillClass(e.eventType)}>
                {e.eventType.charAt(0) + e.eventType.slice(1).toLowerCase()}
              </span>
              <span className="event-actor">{e.actor}</span>
            </div>
            {e.eventType === 'UPDATED' && changeCount > 0 && e.changes && (
              <details className="event-changes">
                <summary>{changeCount} change{changeCount === 1 ? '' : 's'}</summary>
                <ul className="event-diff">
                  {Object.entries(e.changes).map(([field, pair]) => (
                    <li key={field} className="event-diff-row" data-testid="event-change">
                      <span className="event-diff-field">{field}</span>
                      <span className="event-diff-sep">:</span>
                      <span className="event-diff-before">&quot;{pair[0]}&quot;</span>
                      <span className="event-diff-arrow"> → </span>
                      <span className="event-diff-after">&quot;{pair[1]}&quot;</span>
                    </li>
                  ))}
                </ul>
              </details>
            )}
          </li>
        )
      })}
    </ul>
  )
}
