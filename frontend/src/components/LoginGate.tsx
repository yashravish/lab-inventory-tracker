import { useState, type FormEvent } from 'react'
import { api, UnauthorizedError } from '../api'
import type { AuthUser } from '../types'

interface Props {
  onAuthenticated: (user: AuthUser) => void
}

export function LoginGate({ onAuthenticated }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Enter a username and password.')
      return
    }
    setBusy(true)
    try {
      const user = await api.auth.login(username.trim(), password)
      setError(null)
      onAuthenticated(user)
    } catch (err) {
      setError(
        err instanceof UnauthorizedError
          ? err.message
          : err instanceof Error
            ? err.message
            : 'Sign in failed.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-overlay" role="dialog" aria-modal="true" aria-labelledby="login-heading">
      <form className="login-card" onSubmit={handleSubmit}>
        <div className="login-card-head">
          <h2 id="login-heading" className="login-heading">Sign in</h2>
        </div>

        <div className="login-hint" role="note">
          <span className="login-hint-label">Demo credentials</span>
          <div className="login-hint-row">
            <span className="login-hint-key">Username</span>
            <code>yash.s</code>
          </div>
          <div className="login-hint-row">
            <span className="login-hint-key">Password</span>
            <code>labtech</code>
          </div>
        </div>

        {error && (
          <div className="error-banner" role="alert" style={{ marginBottom: 12 }}>
            <span>{error}</span>
          </div>
        )}

        <div className="field">
          <label htmlFor="login-username">Username</label>
          <input
            id="login-username"
            name="username"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={busy}
            autoFocus
          />
        </div>

        <div className="field">
          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={busy}
          />
        </div>

        <button type="submit" className="btn btn-primary login-submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
