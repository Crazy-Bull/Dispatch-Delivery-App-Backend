import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { SEED_USERS } from '../types';

type AuthMode = 'login' | 'signup';

export function AuthPanel() {
  const { login, signUp, logout, user, isAuthenticated, authError, clearAuthError } = useAuth();
  const [mode, setMode] = useState<AuthMode>('login');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    name: '',
    address: '100 Valencia St, San Francisco, CA',
    email: '',
    password: '',
  });

  const switchMode = (next: AuthMode) => {
    setMode(next);
    clearAuthError();
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    clearAuthError();
    try {
      if (mode === 'login') {
        await login(form.email, form.password);
      } else {
        await signUp(form.name, form.address, form.email, form.password);
      }
    } catch {
      // error surfaced via authError
    } finally {
      setLoading(false);
    }
  };

  const fillSeedUser = (email: string, password: string) => {
    setMode('login');
    setForm((prev) => ({ ...prev, email, password }));
    clearAuthError();
  };

  if (isAuthenticated && user) {
    return (
      <section className="panel">
        <h2>Signed in</h2>
        <div className="user-card">
          <p><strong>{user.name}</strong></p>
          <p className="muted">{user.email}</p>
          <p className="muted">{user.address}</p>
          <p className="badge">User ID: {user.id}</p>
        </div>
        <button type="button" className="btn secondary" onClick={logout}>
          Log out
        </button>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="tabs">
        <button
          type="button"
          className={mode === 'login' ? 'tab active' : 'tab'}
          onClick={() => switchMode('login')}
        >
          Log in
        </button>
        <button
          type="button"
          className={mode === 'signup' ? 'tab active' : 'tab'}
          onClick={() => switchMode('signup')}
        >
          Sign up
        </button>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        {mode === 'signup' && (
          <>
            <label>
              Name
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Jane Doe"
              />
            </label>
            <label>
              Address
              <input
                required
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />
            </label>
          </>
        )}
        <label>
          Email
          <input
            required
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="you@example.com"
          />
        </label>
        <label>
          Password
          <input
            required
            type="password"
            minLength={6}
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
        </label>

        {authError && <p className="error">{authError}</p>}

        <button type="submit" className="btn primary" disabled={loading}>
          {loading ? 'Working…' : mode === 'login' ? 'Log in' : 'Create account'}
        </button>
      </form>

      <div className="seed-users">
        <p className="muted">Quick fill (seed users, password: password123):</p>
        <div className="seed-buttons">
          {SEED_USERS.map((seed) => (
            <button
              key={seed.email}
              type="button"
              className="btn ghost"
              onClick={() => fillSeedUser(seed.email, seed.password)}
            >
              {seed.label}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
