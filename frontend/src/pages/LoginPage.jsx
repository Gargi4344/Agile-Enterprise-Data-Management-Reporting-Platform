import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { ApiError, NetworkError } from '../api/client.js';
import './LoginPage.css';

const DEMO_ACCOUNTS = [
  { role: 'Admin', email: 'admin@demo.aedmp.local', password: 'DemoAdmin!2026' },
  { role: 'Analyst', email: 'analyst@demo.aedmp.local', password: 'DemoAnalyst!2026' },
  { role: 'Manager', email: 'manager@demo.aedmp.local', password: 'DemoManager!2026' },
  { role: 'Operator', email: 'operator@demo.aedmp.local', password: 'DemoOperator!2026' },
];

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function LoginPage() {
  const { login, isAuthenticated, loginStatus, sessionExpiredNotice, dismissSessionExpiredNotice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState({});
  const [submitError, setSubmitError] = useState(null);

  if (isAuthenticated) {
    const redirectTo = location.state?.from?.pathname || '/overview';
    return <Navigate to={redirectTo} replace />;
  }

  const errors = {
    email: !email ? 'Email is required.' : !EMAIL_PATTERN.test(email) ? 'Enter a valid email address.' : null,
    password: !password ? 'Password is required.' : null,
  };
  const hasErrors = Boolean(errors.email || errors.password);

  async function handleSubmit(e) {
    e.preventDefault();
    setTouched({ email: true, password: true });
    setSubmitError(null);
    if (hasErrors) return;

    try {
      await login(email, password);
      navigate(location.state?.from?.pathname || '/overview', { replace: true });
    } catch (err) {
      if (err instanceof NetworkError) {
        setSubmitError({ kind: 'network', message: err.message });
      } else if (err instanceof ApiError) {
        setSubmitError({ kind: 'api', message: err.message, fieldErrors: err.fieldErrors });
      } else {
        setSubmitError({ kind: 'unknown', message: 'Something went wrong. Please try again.' });
      }
    }
  }

  return (
    <div className="login-page">
      <div className="login-card card">
        <div className="login-brand">
          <span className="top-nav-mark" aria-hidden="true">
            ▣
          </span>
          <h1>AEDMRP</h1>
        </div>
        <p className="text-secondary" style={{ marginTop: -8, marginBottom: 18 }}>
          Agile Enterprise Data Management &amp; Reporting Platform
        </p>

        {sessionExpiredNotice && (
          <div className="banner banner-error" role="alert">
            Your session expired. Please log in again.
            <button type="button" className="link-button" style={{ marginLeft: 8 }} onClick={dismissSessionExpiredNotice}>
              Dismiss
            </button>
          </div>
        )}

        {submitError && submitError.kind === 'network' && (
          <div className="banner banner-error" role="alert">
            {submitError.message}
          </div>
        )}
        {submitError && submitError.kind !== 'network' && (
          <div className="banner banner-error" role="alert">
            <strong>{submitError.message}</strong>
            {submitError.fieldErrors?.length > 0 && (
              <ul>
                {submitError.fieldErrors.map((fe, i) => (
                  <li key={i}>
                    {fe.field}: {fe.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, email: true }))}
              aria-invalid={touched.email && Boolean(errors.email)}
              aria-describedby={touched.email && errors.email ? 'email-error' : undefined}
            />
            {touched.email && errors.email && (
              <span className="field-error" id="email-error">
                {errors.email}
              </span>
            )}
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, password: true }))}
              aria-invalid={touched.password && Boolean(errors.password)}
              aria-describedby={touched.password && errors.password ? 'password-error' : undefined}
            />
            {touched.password && errors.password && (
              <span className="field-error" id="password-error">
                {errors.password}
              </span>
            )}
          </div>
          <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loginStatus === 'loading'}>
            {loginStatus === 'loading' ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <hr className="hairline" />

        <div className="demo-accounts">
          <p className="text-muted" style={{ fontSize: '0.78rem', marginBottom: 8 }}>
            Demo accounts (read-only reference — type these into the form above):
          </p>
          <table>
            <thead>
              <tr>
                <th>Role</th>
                <th>Email</th>
                <th>Password</th>
              </tr>
            </thead>
            <tbody>
              {DEMO_ACCOUNTS.map((acct) => (
                <tr key={acct.email}>
                  <td>{acct.role}</td>
                  <td>{acct.email}</td>
                  <td>{acct.password}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
