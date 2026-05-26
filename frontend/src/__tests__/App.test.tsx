import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App'
import { api, ConflictError } from '../api'
import type { DashboardSummary, PageResponse, Reagent, ReagentInput } from '../types'

vi.mock('../api', async () => {
  class ConflictError extends Error {
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
  class UnauthorizedError extends Error {
    constructor(message = 'Unauthorized') {
      super(message)
      this.name = 'UnauthorizedError'
    }
  }
  return {
    ConflictError,
    UnauthorizedError,
    api: {
      listReagents: vi.fn(),
      getReagent: vi.fn(),
      createReagent: vi.fn(),
      updateReagent: vi.fn(),
      deleteReagent: vi.fn(),
      summary: vi.fn(),
      exportCsvUrl: vi.fn(() => '/api/reagents/export.csv'),
      importCsv: vi.fn(),
      auth: {
        me: vi.fn(),
        login: vi.fn(),
        logout: vi.fn(),
      },
    },
  }
})

const mockedApi = vi.mocked(api, true)

const SAMPLE: Reagent[] = [
  {
    id: 1,
    name: 'Ethanol',
    supplier: 'Fisher Sci',
    quantity: 12.5,
    unit: 'L',
    storageLocation: 'Lab-2 / Flammables',
    expirationDate: '2027-01-15',
    minimumQuantity: 5,
    lotNumber: 'LOT-2024-E113',
    casNumber: '64-17-5',
    hazardClass: 'FLAMMABLE',
    version: 0,
    lowStock: false,
    expired: false,
    status: 'IN_STOCK',
  },
  {
    id: 2,
    name: 'Methanol',
    supplier: 'Honeywell',
    quantity: 0.5,
    unit: 'L',
    storageLocation: 'Lab-2 / Flammables',
    expirationDate: '2026-04-01',
    minimumQuantity: 5,
    lotNumber: null,
    casNumber: null,
    hazardClass: 'NONE',
    version: 0,
    lowStock: true,
    expired: false,
    status: 'LOW_STOCK',
  },
]

const pageOf = (
  rows: Reagent[],
  opts: { page?: number; size?: number; totalElements?: number } = {},
): PageResponse<Reagent> => {
  const size = opts.size ?? 10
  const total = opts.totalElements ?? rows.length
  return {
    content: rows,
    totalElements: total,
    totalPages: total === 0 ? 0 : Math.max(1, Math.ceil(total / size)),
    number: opts.page ?? 0,
    size,
  }
}

const SUMMARY: DashboardSummary = {
  totalReagents: 2,
  lowStockCount: 1,
  expiredCount: 0,
  inStockCount: 1,
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedApi.auth.me.mockResolvedValue({
    username: 'yash.s',
    displayName: 'Lab Tech',
    role: 'LAB_TECH',
  })
  mockedApi.auth.login.mockResolvedValue({
    username: 'yash.s',
    displayName: 'Lab Tech',
    role: 'LAB_TECH',
  })
  mockedApi.auth.logout.mockResolvedValue(undefined)
  mockedApi.listReagents.mockResolvedValue(pageOf(SAMPLE))
  mockedApi.summary.mockResolvedValue(SUMMARY)
  mockedApi.createReagent.mockImplementation(async (input: ReagentInput) => ({
    id: 99,
    name: input.name,
    supplier: input.supplier ?? null,
    quantity: Number(input.quantity),
    unit: input.unit ?? null,
    storageLocation: input.storageLocation ?? null,
    expirationDate: input.expirationDate ?? null,
    minimumQuantity: Number(input.minimumQuantity ?? 5),
    lotNumber: input.lotNumber ?? null,
    casNumber: input.casNumber ?? null,
    hazardClass: input.hazardClass ?? 'NONE',
    version: 0,
    lowStock: false,
    expired: false,
    status: 'IN_STOCK',
  }))
  mockedApi.deleteReagent.mockResolvedValue(undefined)
})

describe('App', () => {
  it('renders the app title', async () => {
    render(<App />)
    expect(await screen.findByText('Lab Inventory Tracker')).toBeInTheDocument()
  })

  it('renders dashboard cards from the summary API', async () => {
    render(<App />)
    const cards = await screen.findByTestId('dashboard-cards')
    expect(within(cards).getByText('Total reagents')).toBeInTheDocument()
    expect(within(cards).getByText('Low stock')).toBeInTheDocument()
    expect(within(cards).getByText('Expired')).toBeInTheDocument()
    expect(within(cards).getByText('In stock')).toBeInTheDocument()
    await waitFor(() => {
      const totalCard = within(cards).getByLabelText('Total reagents')
      expect(within(totalCard).getByText('2')).toBeInTheDocument()
    })
  })

  it('renders reagents in the table', async () => {
    render(<App />)
    expect(await screen.findByText('Ethanol')).toBeInTheDocument()
    expect(screen.getByText('Methanol')).toBeInTheDocument()
    const table = screen.getByTestId('reagent-table')
    expect(within(table).getByText('Low stock')).toBeInTheDocument()
    expect(within(table).getByText('In stock')).toBeInTheDocument()
    expect(within(table).getByText('CAS 64-17-5')).toBeInTheDocument()
  })

  it('shows the FLAM hazard tag only for non-NONE hazard rows', async () => {
    render(<App />)
    await screen.findByText('Ethanol')
    const table = screen.getByTestId('reagent-table')

    const ethanolRow = within(table).getByText('Ethanol').closest('tr')!
    expect(within(ethanolRow).getByText('FLAM')).toBeInTheDocument()

    const methanolRow = within(table).getByText('Methanol').closest('tr')!
    expect(within(methanolRow).queryByText('FLAM')).not.toBeInTheDocument()
    expect(within(methanolRow).queryByText('CORR')).not.toBeInTheDocument()
    expect(within(methanolRow).queryByText('TOX')).not.toBeInTheDocument()
  })

  it('submits a new reagent via the drawer form', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Ethanol')

    await user.click(screen.getByRole('button', { name: /^Add$/i }))
    await user.type(screen.getByLabelText(/^Name/i), 'Buffer X')
    await user.type(screen.getByLabelText(/^Quantity/i), '10')
    await user.click(screen.getByRole('button', { name: /Save reagent/i }))

    await waitFor(() => {
      expect(mockedApi.createReagent).toHaveBeenCalledTimes(1)
    })
    const call = mockedApi.createReagent.mock.calls[0][0]
    expect(call.name).toBe('Buffer X')
    expect(Number(call.quantity)).toBe(10)
  })

  it('tints the row for an EXPIRED reagent with the row-expired class', async () => {
    mockedApi.listReagents.mockResolvedValueOnce(pageOf([
      {
        id: 3,
        name: 'Old Acetone',
        supplier: 'Sigma',
        quantity: 2,
        unit: 'L',
        storageLocation: 'Lab-1 / Flammables',
        expirationDate: '2024-01-01',
        minimumQuantity: 5,
        lotNumber: null,
        casNumber: null,
        hazardClass: 'NONE',
        version: 0,
        lowStock: false,
        expired: true,
        status: 'EXPIRED',
      },
    ]))
    render(<App />)
    const cell = await screen.findByText('Old Acetone')
    expect(cell.closest('tr')).toHaveClass('row-expired')
  })

  it('filters reagents by status via the styled status menu', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Ethanol')

    const trigger = screen.getByLabelText('Status filter')
    expect(trigger).toHaveTextContent('All')
    expect(trigger).toHaveAttribute('aria-expanded', 'false')

    await user.click(trigger)

    const listbox = await screen.findByRole('listbox')
    expect(within(listbox).getByRole('option', { name: 'All' })).toBeInTheDocument()
    expect(within(listbox).getByRole('option', { name: 'In stock' })).toBeInTheDocument()
    expect(within(listbox).getByRole('option', { name: 'Low stock' })).toBeInTheDocument()
    expect(within(listbox).getByRole('option', { name: 'Expired' })).toBeInTheDocument()

    await user.click(within(listbox).getByRole('option', { name: 'Low stock' }))

    await waitFor(() => {
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    })
    expect(trigger).toHaveTextContent('Low stock')

    const table = screen.getByTestId('reagent-table')
    expect(within(table).getByText('Methanol')).toBeInTheDocument()
    expect(within(table).queryByText('Ethanol')).not.toBeInTheDocument()
  })

  it('calls delete API when Delete button is clicked', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<App />)
    await screen.findByText('Ethanol')

    await user.click(screen.getByRole('button', { name: /Delete Ethanol/i }))
    await waitFor(() => {
      expect(mockedApi.deleteReagent).toHaveBeenCalledWith(1)
    })
    confirmSpy.mockRestore()
  })

  it('refetches with page=1 when Next is clicked', async () => {
    mockedApi.listReagents.mockResolvedValue(
      pageOf(SAMPLE, { page: 0, size: 25, totalElements: 80 }),
    )
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Ethanol')

    const firstCall = mockedApi.listReagents.mock.calls[0][0]!
    expect(firstCall.page).toBe(0)

    mockedApi.listReagents.mockClear()
    mockedApi.listReagents.mockResolvedValue(
      pageOf(SAMPLE, { page: 1, size: 25, totalElements: 80 }),
    )

    await user.click(screen.getByRole('button', { name: /Next page/i }))

    await waitFor(() => {
      expect(mockedApi.listReagents).toHaveBeenCalled()
    })
    const calls = mockedApi.listReagents.mock.calls
    const nextCall = calls[calls.length - 1]![0]!
    expect(nextCall.page).toBe(1)
  })

  it('disables Prev on page 0 and Next when on the only page', async () => {
    mockedApi.listReagents.mockResolvedValue(
      pageOf(SAMPLE, { page: 0, size: 25, totalElements: 2 }),
    )
    render(<App />)
    await screen.findByText('Ethanol')

    expect(screen.getByRole('button', { name: /Previous page/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Next page/i })).toBeDisabled()
  })

  it('shows the import success banner with the counts from a mocked import response', async () => {
    const user = userEvent.setup()
    mockedApi.importCsv.mockResolvedValueOnce({
      created: 3,
      updated: 1,
      skipped: 0,
      errors: [],
    })
    render(<App />)
    await screen.findByText('Ethanol')

    const fileInput = screen.getByLabelText('Import CSV file') as HTMLInputElement
    const file = new File(
      ['name,quantity\nFoo,10\n'],
      'reagents.csv',
      { type: 'text/csv' },
    )
    await user.upload(fileInput, file)

    await waitFor(() => {
      expect(mockedApi.importCsv).toHaveBeenCalledTimes(1)
    })
    const passedFile = mockedApi.importCsv.mock.calls[0][0]
    expect(passedFile).toBeInstanceOf(File)
    expect(passedFile.name).toBe('reagents.csv')

    const banner = await screen.findByRole('status')
    expect(banner).toHaveTextContent('Imported: 3 created, 1 updated, 0 skipped.')
  })

  it('shows a conflict banner with a Reload button when update returns 409', async () => {
    const user = userEvent.setup()
    mockedApi.updateReagent.mockRejectedValueOnce(
      new ConflictError({ message: 'Reagent was modified by another user.', currentVersion: 1 }),
    )
    const freshRow: Reagent = {
      ...SAMPLE[0],
      quantity: 7,
      version: 1,
    }
    mockedApi.getReagent.mockResolvedValueOnce(freshRow)

    render(<App />)
    await screen.findByText('Ethanol')

    await user.click(screen.getByRole('button', { name: /Edit Ethanol/i }))
    await user.click(screen.getByRole('button', { name: /Save reagent/i }))

    expect(await screen.findByText(/edited by someone else/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^Reload$/ }))

    await waitFor(() => {
      expect(mockedApi.getReagent).toHaveBeenCalledWith(1)
    })
    await waitFor(() => {
      expect(screen.queryByText(/edited by someone else/i)).not.toBeInTheDocument()
    })
  })
})
