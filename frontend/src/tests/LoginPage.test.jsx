import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext.jsx';
import { LoginPage } from '../pages/LoginPage.jsx';

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shows required-field errors when submitting an empty form', async () => {
    renderLoginPage();
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it('shows an email format error for an invalid address', async () => {
    renderLoginPage();
    const user = userEvent.setup();
    const emailInput = screen.getByLabelText(/email/i);

    await user.type(emailInput, 'not-an-email');
    fireEvent.blur(emailInput);

    expect(await screen.findByText(/enter a valid email address/i)).toBeInTheDocument();
  });

  it('lists the demo accounts as read-only reference text', () => {
    renderLoginPage();
    expect(screen.getByText('admin@demo.aedmp.local')).toBeInTheDocument();
    expect(screen.getByText('analyst@demo.aedmp.local')).toBeInTheDocument();
    expect(screen.getByText('manager@demo.aedmp.local')).toBeInTheDocument();
    expect(screen.getByText('operator@demo.aedmp.local')).toBeInTheDocument();
  });

  it('submits credentials and surfaces a server error message on failed login', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.', requestId: 'req-1' }),
    });

    renderLoginPage();
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/email/i), 'admin@demo.aedmp.local');
    await user.type(screen.getByLabelText(/password/i), 'wrong-password');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/login'),
      expect.objectContaining({ method: 'POST' })
    );
  });
});
