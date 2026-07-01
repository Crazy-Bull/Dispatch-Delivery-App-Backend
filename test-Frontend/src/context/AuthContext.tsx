import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { login as loginApi, signUp as signUpApi } from '../api';
import { ApiError } from '../api/client';
import type { User } from '../types';

const TOKEN_KEY = 'dispatch_test_token';
const USER_KEY = 'dispatch_test_user';

interface AuthContextValue {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  signUp: (name: string, address: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  authError: string | null;
  clearAuthError: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<User | null>(() => loadStoredUser());
  const [authError, setAuthError] = useState<string | null>(null);

  const persistSession = useCallback((nextToken: string, nextUser: User) => {
    localStorage.setItem(TOKEN_KEY, nextToken);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(nextToken);
    setUser(nextUser);
    setAuthError(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    try {
      const response = await loginApi({ email, password });
      persistSession(response.token, response.user);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Login failed';
      setAuthError(message);
      throw err;
    }
  }, [persistSession]);

  const signUp = useCallback(
    async (name: string, address: string, email: string, password: string) => {
      try {
        const response = await signUpApi({ name, address, email, password });
        persistSession(response.token, response.user);
      } catch (err) {
        const message = err instanceof ApiError ? err.message : 'Sign up failed';
        setAuthError(message);
        throw err;
      }
    },
    [persistSession],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
    setAuthError(null);
  }, []);

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      login,
      signUp,
      logout,
      authError,
      clearAuthError: () => setAuthError(null),
    }),
    [token, user, login, signUp, logout, authError],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
