export type ReagentStatus = 'IN_STOCK' | 'LOW_STOCK' | 'EXPIRED'

export type HazardClass = 'FLAMMABLE' | 'CORROSIVE' | 'TOXIC' | 'NONE'

export interface Reagent {
  id: number
  name: string
  supplier: string | null
  quantity: number | string
  unit: string | null
  storageLocation: string | null
  expirationDate: string | null
  minimumQuantity: number | string
  lotNumber: string | null
  casNumber: string | null
  hazardClass: HazardClass
  version: number
  lowStock: boolean
  expired: boolean
  status: ReagentStatus
}

export interface ReagentInput {
  name: string
  supplier?: string
  quantity: number | string
  unit?: string
  storageLocation?: string
  expirationDate?: string
  minimumQuantity?: number | string
  lotNumber?: string
  casNumber?: string
  hazardClass?: HazardClass
  version?: number
}

export interface DashboardSummary {
  totalReagents: number
  lowStockCount: number
  expiredCount: number
  inStockCount: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ImportError {
  row: number
  message: string
}

export interface ImportResult {
  created: number
  updated: number
  skipped: number
  errors: ImportError[]
}

export type ReagentEventType = 'CREATED' | 'UPDATED' | 'DELETED'

export interface ReagentEvent {
  id: number
  reagentId: number
  reagentName: string
  eventType: ReagentEventType
  changes: Record<string, [string, string]> | null
  actor: string
  createdAt: string
}

export interface AuthUser {
  username: string
  displayName: string
  role: string
}
