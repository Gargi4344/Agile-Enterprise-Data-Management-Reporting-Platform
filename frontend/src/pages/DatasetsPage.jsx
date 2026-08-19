import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { usePaginatedFetch } from '../hooks/usePaginatedFetch.js';
import { datasetsApi } from '../api/datasets.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState, NoResultsState } from '../components/states/EmptyState.jsx';
import { ValidationErrorBanner } from '../components/states/ErrorState.jsx';
import { Pagination } from '../components/common/Pagination.jsx';
import { Modal } from '../components/common/Modal.jsx';
import { RoleGate } from '../components/common/RoleGate.jsx';
import { StatusBadge } from '../components/common/StatusBadge.jsx';
import { ApiError } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

function useDebouncedValue(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function DatasetsPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');
  const search = useDebouncedValue(searchInput);
  const [createOpen, setCreateOpen] = useState(false);

  const { content, status, error, page, setPage, totalPages, reload } = usePaginatedFetch(
    ({ page: p, size, search: s }) => datasetsApi.list({ page: p, size, search: s }),
    { search }
  );

  const isForbidden = error instanceof ApiError && error.status === 403;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Dataset registry</h1>
          <p className="page-subtitle">Register datasets, browse lineage, and track version status.</p>
        </div>
        <RoleGate permission="DATASET_UPLOAD" fallbackTitle="Requires DATASET_UPLOAD permission">
          <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
            + Register dataset
          </button>
        </RoleGate>
      </div>

      <div className="card">
        <div className="row" style={{ marginBottom: 16 }}>
          <input
            type="search"
            placeholder="Search datasets by name…"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            style={{
              flex: 1,
              padding: '9px 12px',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border-strong)',
              background: 'var(--surface-2)',
              color: 'var(--text-primary)',
            }}
            aria-label="Search datasets"
          />
        </div>

        {status === 'loading' && <LoadingState rows={5} />}
        {status === 'error' && isForbidden && <PermissionDenied message="You don't have permission to view the dataset registry." />}
        {status === 'error' && !isForbidden && <ErrorState error={error} onRetry={reload} />}
        {status === 'success' && content.length === 0 && search && (
          <NoResultsState query={search} onClear={() => setSearchInput('')} />
        )}
        {status === 'success' && content.length === 0 && !search && (
          <EmptyState
            title="No datasets registered yet"
            description="Register your first dataset to start tracking versions and quality."
            icon="🗂️"
          />
        )}
        {status === 'success' && content.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Status</th>
                  <th>Owner</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {content.map((ds) => (
                  <tr key={ds.id} onClick={() => navigate(`/datasets/${ds.id}`)} style={{ cursor: 'pointer' }}>
                    <td>
                      <Link to={`/datasets/${ds.id}`} onClick={(e) => e.stopPropagation()}>
                        {ds.name}
                      </Link>
                    </td>
                    <td className="text-secondary" style={{ maxWidth: 320, whiteSpace: 'normal' }}>
                      {ds.description || '—'}
                    </td>
                    <td>{ds.status ? <StatusBadge status={ds.status} /> : '—'}</td>
                    <td>{ds.ownerName || ds.owner || '—'}</td>
                    <td>
                      <Link to={`/datasets/${ds.id}`} className="btn btn-sm" onClick={(e) => e.stopPropagation()}>
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {status === 'success' && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}
      </div>

      {createOpen && can('DATASET_UPLOAD') && (
        <CreateDatasetModal
          onClose={() => setCreateOpen(false)}
          onCreated={(ds) => {
            setCreateOpen(false);
            reload();
            if (ds?.id) navigate(`/datasets/${ds.id}`);
          }}
        />
      )}
    </div>
  );
}

function CreateDatasetModal({ onClose, onCreated }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);

  const nameError = !name.trim() ? 'Dataset name is required.' : null;

  const handleSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      setTouched(true);
      if (nameError) return;
      setSubmitting(true);
      setError(null);
      try {
        const created = await datasetsApi.create({ name: name.trim(), description: description.trim() });
        onCreated(created);
      } catch (err) {
        setError(err);
      } finally {
        setSubmitting(false);
      }
    },
    [name, description, nameError, onCreated]
  );

  return (
    <Modal title="Register dataset" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        <div className="field">
          <label htmlFor="ds-name">Name</label>
          <input id="ds-name" value={name} onChange={(e) => setName(e.target.value)} aria-invalid={touched && Boolean(nameError)} />
          {touched && nameError && <span className="field-error">{nameError}</span>}
        </div>
        <div className="field">
          <label htmlFor="ds-description">Description</label>
          <textarea id="ds-description" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Registering…' : 'Register dataset'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
