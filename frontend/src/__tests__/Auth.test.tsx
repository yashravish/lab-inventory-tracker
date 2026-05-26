import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App'
import { api } from '../api'
import type { PageResponse, Reagent } from '../types'

vi.mock('../api', async () => {
  class ConflictError extends Error {
    body: unknown
    constructor(body: unknown) {
      super('conflict')
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

const emptyPage: PageResponse<Reagent> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 25,
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedApi.listReagents.mockResolvedValue(emptyPage)
  mockedApi.summary.mockResolvedValue({
    totalReagents: 0,
    lowStockCount: 0,
    expiredCount: 0,
    inStockCount: 0,
  })
})

describe('Auth flow', () => {
  it('renders the login gate when /auth/me throws, then mounts the app after successful login', async () => {
    const UnauthorizedError = (await import('../api')).UnauthorizedError
    mockedApi.auth.me.mockRejectedValueOnce(new UnauthorizedError('Unauthorized'))
    mockedApi.auth.login.mockResolvedValueOnce({
      username: 'yash.s',
      displayName: 'Lab Tech',
      role: 'LAB_TECH',
    })

    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Inventory' })).not.toBeInTheDocument()

    await user.type(screen.getByLabelText('Username'), 'yash.s')
    await user.type(screen.getByLabelText('Password'), 'labtech')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(mockedApi.auth.login).toHaveBeenCalledWith('yash.s', 'labtech')
    })

    expect(await screen.findByRole('heading', { name: 'Inventory' })).toBeInTheDocument()
    const chip = screen.getByRole('button', { name: /yash\.s/ })
    expect(chip).toBeInTheDocument()
  })

  it('shows the inline error and stays on the login gate after a 401', async () => {
    const UnauthorizedError = (await import('../api')).UnauthorizedError
    mockedApi.auth.me.mockRejectedValueOnce(new UnauthorizedError('Unauthorized'))
    mockedApi.auth.login.mockRejectedValueOnce(
      new UnauthorizedError('Invalid username or password.'),
    )

    const user = userEvent.setup()
    render(<App />)

    await screen.findByRole('heading', { name: 'Sign in' })
    await user.type(screen.getByLabelText('Username'), 'yash.s')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('signs out via the user-chip menu and returns to the login gate', async () => {
    mockedApi.auth.me.mockResolvedValueOnce({
      username: 'yash.s',
      displayName: 'Lab Tech',
      role: 'LAB_TECH',
    })
    mockedApi.auth.logout.mockResolvedValueOnce(undefined)

    const user = userEvent.setup()
    render(<App />)

    await screen.findByRole('heading', { name: 'Inventory' })
    const chip = screen.getByRole('button', { name: /yash\.s/ })
    await user.click(chip)

    const menu = await screen.findByRole('menu')
    await user.click(within(menu).getByRole('menuitem', { name: 'Sign out' }))

    await waitFor(() => {
      expect(mockedApi.auth.logout).toHaveBeenCalledTimes(1)
    })
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })
})
