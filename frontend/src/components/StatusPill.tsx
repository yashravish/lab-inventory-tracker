import type { ReagentStatus } from '../types'

const LABELS: Record<ReagentStatus, string> = {
  IN_STOCK: 'In stock',
  LOW_STOCK: 'Low stock',
  EXPIRED: 'Expired',
}

const CLASSES: Record<ReagentStatus, string> = {
  IN_STOCK: 'pill pill-ok',
  LOW_STOCK: 'pill pill-warn',
  EXPIRED: 'pill pill-err',
}

export function StatusPill({ status }: { status: ReagentStatus }) {
  return <span className={CLASSES[status]}>{LABELS[status]}</span>
}
