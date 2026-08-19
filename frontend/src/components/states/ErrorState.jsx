import { ApiError, NetworkError } from '../../api/client.js';

export function ErrorState({ error, onRetry, title }) {
  const isNetwork = error instanceof NetworkError;
  const isServer = error instanceof ApiError && error.status >= 500;
  const heading = title || (isNetwork ? 'Connection problem' : isServer ? 'Server error' : 'Something went wrong');
  const requestId = error instanceof ApiError ? error.requestId : undefined;

  return (
    <div className="state-block state-block-center" role="alert">
      <div className="state-icon" aria-hidden="true">
        {isNetwork ? '📡' : '⚠️'}
      </div>
      <h3>{heading}</h3>
      <p className="text-secondary">{error?.message || 'An unexpected error occurred.'}</p>
      {requestId && <p className="text-muted" style={{ fontSize: '0.75rem' }}>Request ID: {requestId}</p>}
      {onRetry && (
        <button type="button" className="btn btn-primary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function ValidationErrorBanner({ error }) {
  if (!error) return null;
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : undefined;
  return (
    <div className="banner banner-error" role="alert">
      <strong>{error.message || 'Please fix the errors below.'}</strong>
      {fieldErrors?.length > 0 && (
        <ul>
          {fieldErrors.map((fe, i) => (
            <li key={i}>
              {fe.field}: {fe.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
