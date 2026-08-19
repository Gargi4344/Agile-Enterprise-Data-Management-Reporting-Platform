import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { EmptyState, NoResultsState } from '../components/states/EmptyState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ApiError, NetworkError } from '../api/client.js';

describe('UI state components', () => {
  it('EmptyState renders a title, description, and optional action', () => {
    render(<EmptyState title="No datasets" description="Register one to get started." action={<button>Register</button>} />);
    expect(screen.getByText('No datasets')).toBeInTheDocument();
    expect(screen.getByText('Register one to get started.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Register' })).toBeInTheDocument();
  });

  it('NoResultsState surfaces the search query and a clear action', () => {
    const onClear = vi.fn();
    render(<NoResultsState query="widgets" onClear={onClear} />);
    expect(screen.getByText(/no results for "widgets"/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /clear filters/i }));
    expect(onClear).toHaveBeenCalledTimes(1);
  });

  it('ErrorState shows a network-specific heading for NetworkError', () => {
    render(<ErrorState error={new NetworkError(new Error('offline'))} />);
    expect(screen.getByText(/connection problem/i)).toBeInTheDocument();
  });

  it('ErrorState shows a server-error heading and request id for 5xx ApiError', () => {
    const error = new ApiError(500, { code: 'INTERNAL', message: 'Something broke', requestId: 'req-42' });
    render(<ErrorState error={error} />);
    expect(screen.getByText(/server error/i)).toBeInTheDocument();
    expect(screen.getByText(/req-42/)).toBeInTheDocument();
  });

  it('ErrorState retry button invokes the onRetry callback', () => {
    const onRetry = vi.fn();
    render(<ErrorState error={new Error('boom')} onRetry={onRetry} />);
    fireEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('PermissionDenied renders a lock icon and default message', () => {
    render(<PermissionDenied />);
    expect(screen.getByText(/permission denied/i)).toBeInTheDocument();
    expect(screen.getByText(/don't have permission to view this page/i)).toBeInTheDocument();
  });

  it('LoadingState renders the requested number of skeleton rows', () => {
    const { container } = render(<LoadingState rows={4} />);
    expect(container.querySelectorAll('.skeleton')).toHaveLength(4);
  });
});
