import { useCallback, useEffect, useRef, useState } from 'react';

const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

/**
 * Wraps a paginated list endpoint that returns the shared envelope
 * {content, page, size, totalElements, totalPages}, tracking page/search state and the
 * loading/success/error status for the caller to render the required UI states.
 */
export function usePaginatedFetch(fetcher, { size = 20, search = '', extraDeps = [] } = {}) {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('loading');
  const [result, setResult] = useState(EMPTY_PAGE);
  const [error, setError] = useState(null);
  const requestRef = useRef(0);

  const load = useCallback(() => {
    const requestId = ++requestRef.current;
    setStatus('loading');
    setError(null);
    fetcher({ page, size, search })
      .then((data) => {
        if (requestRef.current === requestId) {
          setResult(data ?? EMPTY_PAGE);
          setStatus('success');
        }
      })
      .catch((err) => {
        if (requestRef.current === requestId) {
          setError(err);
          setStatus('error');
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, search, ...extraDeps]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [search]);

  return { ...result, status, error, page, setPage, reload: load };
}
