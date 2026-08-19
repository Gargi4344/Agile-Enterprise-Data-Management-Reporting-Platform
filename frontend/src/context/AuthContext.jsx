import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/auth.js';
import { getAuthToken, registerUnauthorizedHandler, setAuthToken } from '../api/client.js';
import { permissionsForRoles } from '../utils/rbac.js';

const USER_STORAGE_KEY = 'aedmp.auth.user';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = sessionStorage.getItem(USER_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  });
  const [status, setStatus] = useState('idle');
  const [sessionExpiredNotice, setSessionExpiredNotice] = useState(false);

  const clearSession = useCallback(() => {
    setAuthToken(null);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    registerUnauthorizedHandler(() => {
      clearSession();
      setSessionExpiredNotice(true);
    });
  }, [clearSession]);

  const login = useCallback(async (email, password) => {
    setStatus('loading');
    try {
      const data = await authApi.login(email, password);
      setAuthToken(data.token);
      sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(data.user));
      setUser(data.user);
      setSessionExpiredNotice(false);
      setStatus('idle');
      return data.user;
    } catch (err) {
      setStatus('idle');
      throw err;
    }
  }, []);

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const permissions = useMemo(() => permissionsForRoles(user?.roles ?? []), [user]);

  const can = useCallback((permission) => permissions.has(permission), [permissions]);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user && getAuthToken()),
      loginStatus: status,
      sessionExpiredNotice,
      dismissSessionExpiredNotice: () => setSessionExpiredNotice(false),
      login,
      logout,
      can,
      roles: user?.roles ?? [],
    }),
    [user, status, sessionExpiredNotice, login, logout, can]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
