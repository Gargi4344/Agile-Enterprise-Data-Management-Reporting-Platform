import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async fetcher and exposes {status, data, error, reload}. status is one of
 * 'loading' | 'success' | 'error' so screens can render the exact required UI state
 * (loading skeleton, error/permission-denied panel, or the loaded content) without
 * ad-hoc booleans scattered per page.
 */
export function useAsync(fetcher, deps = []) {
  const [state, setState] = useState({ status: 'loading', data: null, error: null });
  const requestRef = useRef(0);

  const load = useCallback(() => {
    const requestId = ++requestRef.current;
    setState((prev) => ({ ...prev, status: 'loading', error: null }));
    fetcher()
      .then((data) => {
        if (requestRef.current === requestId) setState({ status: 'success', data, error: null });
      })
      .catch((error) => {
        if (requestRef.current === requestId) setState({ status: 'error', data: null, error });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    load();
  }, [load]);

  return { ...state, reload: load };
}
