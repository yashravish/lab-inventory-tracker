import type { AiQueryResponse, AuthUser, DashboardSummary, ImportResult, PageResponse, Reagent, ReagentEvent, ReagentInput } from './types'

const BASE = import.meta.env?.VITE_API_BASE_URL ?? 'http://localhost:8082'

export class ConflictError extends Error {
  body: unknown
  constructor(body: unknown) {
    const message = (body && typeof body === 'object' && 'message' in body)
      ? String((body as { message: unknown }).message)
      : 'Conflict'
    super(message)
    this.name = 'ConflictError'
    this.body = body
  }
}

export class UnauthorizedError extends Error {
  constructor(message = 'Unauthorized') {
    super(message)
    this.name = 'UnauthorizedError'
  }
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    if (res.status === 409) {
      const body = await res.json().catch(() => ({}))
      throw new ConflictError(body)
    }
    if (res.status === 401) {
      const body = await res.json().catch(() => ({}))
      const message = (body && typeof body === 'object' && 'message' in body)
        ? String((body as { message: unknown }).message)
        : 'Unauthorized'
      throw new UnauthorizedError(message)
    }
    const text = await res.text().catch(() => '')
    throw new Error(`${res.status} ${res.statusText}${text ? `: ${text}` : ''}`)
  }
  if (res.status === 204) return undefined as unknown as T
  return res.json() as Promise<T>
}

function withCreds(init: RequestInit = {}): RequestInit {
  return { credentials: 'include', ...init }
}

export interface ListReagentsOpts {
  search?: string
  page?: number
  size?: number
  sort?: string
}

export const api = {
  listReagents(opts: ListReagentsOpts = {}): Promise<PageResponse<Reagent>> {
    const params = new URLSearchParams()
    if (opts.search && opts.search.trim()) params.set('search', opts.search.trim())
    if (typeof opts.page === 'number') params.set('page', String(opts.page))
    if (typeof opts.size === 'number') params.set('size', String(opts.size))
    if (opts.sort) params.set('sort', opts.sort)
    const qs = params.toString() ? `?${params.toString()}` : ''
    return fetch(`${BASE}/api/reagents${qs}`, withCreds()).then((r) => handle<PageResponse<Reagent>>(r))
  },
  getReagent(id: number): Promise<Reagent> {
    return fetch(`${BASE}/api/reagents/${id}`, withCreds()).then((r) => handle<Reagent>(r))
  },
  createReagent(input: ReagentInput): Promise<Reagent> {
    return fetch(`${BASE}/api/reagents`, withCreds({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })).then((r) => handle<Reagent>(r))
  },
  updateReagent(id: number, input: ReagentInput): Promise<Reagent> {
    return fetch(`${BASE}/api/reagents/${id}`, withCreds({
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })).then((r) => handle<Reagent>(r))
  },
  deleteReagent(id: number): Promise<void> {
    return fetch(`${BASE}/api/reagents/${id}`, withCreds({ method: 'DELETE' })).then((r) => handle<void>(r))
  },
  summary(): Promise<DashboardSummary> {
    return fetch(`${BASE}/api/dashboard/summary`, withCreds()).then((r) => handle<DashboardSummary>(r))
  },
  exportCsvUrl(): string {
    return `${BASE}/api/reagents/export.csv`
  },
  importCsv(file: File): Promise<ImportResult> {
    const form = new FormData()
    form.append('file', file)
    return fetch(`${BASE}/api/reagents/import`, withCreds({
      method: 'POST',
      body: form,
    })).then((r) => handle<ImportResult>(r))
  },
  listEvents(reagentId: number): Promise<ReagentEvent[]> {
    return fetch(`${BASE}/api/reagents/${reagentId}/history`, withCreds()).then((r) => handle<ReagentEvent[]>(r))
  },
  aiQuery(q: string, size?: number): Promise<AiQueryResponse> {
    return fetch(`${BASE}/api/ai/query`, withCreds({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ q, size }),
    })).then((r) => handle<AiQueryResponse>(r))
  },
  auth: {
    me(): Promise<AuthUser> {
      return fetch(`${BASE}/api/auth/me`, withCreds()).then((r) => handle<AuthUser>(r))
    },
    login(username: string, password: string): Promise<AuthUser> {
      return fetch(`${BASE}/api/auth/login`, withCreds({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })).then((r) => handle<AuthUser>(r))
    },
    logout(): Promise<void> {
      return fetch(`${BASE}/api/auth/logout`, withCreds({ method: 'POST' })).then((r) => handle<void>(r))
    },
  },
}
