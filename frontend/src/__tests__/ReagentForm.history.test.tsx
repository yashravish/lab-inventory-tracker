import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ReagentForm } from '../components/ReagentForm'
import { api } from '../api'
import type { Reagent, ReagentEvent } from '../types'

vi.mock('../api', () => ({
  api: {
    listReagents: vi.fn(),
    getReagent: vi.fn(),
    createReagent: vi.fn(),
    updateReagent: vi.fn(),
    deleteReagent: vi.fn(),
    summary: vi.fn(),
    exportCsvUrl: vi.fn(),
    importCsv: vi.fn(),
    listEvents: vi.fn(),
    aiQuery: vi.fn(),
  },
  ConflictError: class ConflictError extends Error {},
}))

const mockedApi = vi.mocked(api, true)

const EDITING: Reagent = {
  id: 7,
  name: 'Ethanol',
  supplier: 'Fisher',
  quantity: 8,
  unit: 'L',
  storageLocation: 'Lab-2',
  expirationDate: '2027-01-15',
  minimumQuantity: 5,
  lotNumber: null,
  casNumber: null,
  hazardClass: 'FLAMMABLE',
  version: 2,
  lowStock: false,
  expired: false,
  status: 'IN_STOCK',
}

const UPDATED_EVENT: ReagentEvent = {
  id: 101,
  reagentId: 7,
  reagentName: 'Ethanol',
  eventType: 'UPDATED',
  changes: { quantity: ['10.0000', '8'] },
  actor: 'yash.s · Lab Tech',
  createdAt: '2026-05-14T17:42:00Z',
}

const CREATED_EVENT: ReagentEvent = {
  id: 100,
  reagentId: 7,
  reagentName: 'Ethanol',
  eventType: 'CREATED',
  changes: null,
  actor: 'yash.s · Lab Tech',
  createdAt: '2026-05-13T09:00:00Z',
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedApi.listEvents.mockResolvedValue([UPDATED_EVENT, CREATED_EVENT])
})

describe('ReagentForm history tab', () => {
  it('fetches history when the History tab is clicked and renders an UPDATED event with one change row', async () => {
    const user = userEvent.setup()
    render(
      <ReagentForm
        open={true}
        editing={EDITING}
        onCancel={() => {}}
        onSubmit={async () => {}}
      />,
    )

    expect(mockedApi.listEvents).not.toHaveBeenCalled()

    await user.click(screen.getByRole('tab', { name: /History/i }))

    await waitFor(() => {
      expect(mockedApi.listEvents).toHaveBeenCalledWith(7)
    })

    const rows = await screen.findAllByTestId('event-row')
    expect(rows).toHaveLength(2)

    await user.click(screen.getByText(/1 change/i))
    const changeRows = await screen.findAllByTestId('event-change')
    expect(changeRows).toHaveLength(1)
    expect(changeRows[0].textContent).toContain('quantity')
    expect(changeRows[0].textContent).toContain('10.0000')
    expect(changeRows[0].textContent).toContain('8')
  })

  it('does not show the History tab when creating a new reagent', () => {
    render(
      <ReagentForm
        open={true}
        editing={null}
        onCancel={() => {}}
        onSubmit={async () => {}}
      />,
    )
    expect(screen.queryByRole('tab', { name: /History/i })).not.toBeInTheDocument()
  })
})
