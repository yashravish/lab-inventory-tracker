import type { DashboardSummary, ReagentStatus } from '../types'

type Filter = 'ALL' | ReagentStatus

interface Props {
  summary: DashboardSummary | null
  filter: Filter
  onFilterChange: (f: Filter) => void
}

export function DashboardCards({ summary, filter, onFilterChange }: Props) {
  const s = summary ?? { totalReagents: 0, lowStockCount: 0, expiredCount: 0, inStockCount: 0 }

  return (
    <div className="stats" data-testid="dashboard-cards">
      <button
        className={`stat ${filter === 'ALL' ? 'active' : ''}`}
        onClick={() => onFilterChange('ALL')}
        aria-label="Total reagents"
      >
        <div className="stat-label">Total reagents</div>
        <div className="stat-value">{s.totalReagents}</div>
        <div className="stat-sub">All inventory items</div>
      </button>
      <button
        className={`stat ${filter === 'LOW_STOCK' ? 'active' : ''}`}
        onClick={() => onFilterChange('LOW_STOCK')}
        aria-label="Low stock"
      >
        <div className="stat-label">Low stock</div>
        <div className="stat-value warn">{s.lowStockCount}</div>
        <div className="stat-sub">At or below minimum</div>
      </button>
      <button
        className={`stat ${filter === 'EXPIRED' ? 'active' : ''}`}
        onClick={() => onFilterChange('EXPIRED')}
        aria-label="Expired"
      >
        <div className="stat-label">Expired</div>
        <div className="stat-value err">{s.expiredCount}</div>
        <div className="stat-sub">Past expiration date</div>
      </button>
      <button
        className={`stat ${filter === 'IN_STOCK' ? 'active' : ''}`}
        onClick={() => onFilterChange('IN_STOCK')}
        aria-label="In stock"
      >
        <div className="stat-label">In stock</div>
        <div className="stat-value ok">{s.inStockCount}</div>
        <div className="stat-sub">Healthy items</div>
      </button>
    </div>
  )
}
