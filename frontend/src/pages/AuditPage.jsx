import { useState } from 'react';
import { usePaginatedFetch } from '../hooks/usePaginatedFetch.js';
import { auditApi } from '../api/audit.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState, NoResultsState } from '../components/states/EmptyState.jsx';
import { Pagination } from '../components/common/Pagination.jsx';
import { formatDateTime } from '../utils/format.js';
import { useAuth } from '../context/AuthContext.jsx';

export function AuditPage() {
  const { can } = useAuth();
  const allowed = can('AUDIT_VIEW_ALL') || can('AUDIT_VIEW_LIMITED');
  const [targetType, setTargetType] = useState('');
  const [targetId, setTargetId] = useState('');
  const [actorId, setActorId] = useState('');

  const { content, status, error, page, setPage, totalPages, reload } = usePaginatedFetch(
    ({ page: p, size }) => auditApi.list({ targetType: targetType || undefined, targetId: targetId || undefined, actorId: actorId || undefined, page: p, size }),
    { size: 20, extraDeps: [targetType, targetId, actorId] }
  );

  const hasFilters = Boolean(targetType || targetId || actorId);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Audit trail</h1>
          <p className="page-subtitle">
            {can('AUDIT_VIEW_ALL') ? 'Full audit history across the platform.' : 'Audit events scoped to your owned datasets and reports.'}
          </p>
        </div>
      </div>

      <div className="card">
        {!allowed && <PermissionDenied message="Your role does not have audit trail access. Contact an administrator if you believe this is a mistake." />}

        {allowed && (
          <>
            <div className="row" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
              <input
                placeholder="Target type (e.g. DATASET)"
                value={targetType}
                onChange={(e) => setTargetType(e.target.value)}
                style={inputStyle}
              />
              <input placeholder="Target ID" value={targetId} onChange={(e) => setTargetId(e.target.value)} style={inputStyle} />
              <input placeholder="Actor ID" value={actorId} onChange={(e) => setActorId(e.target.value)} style={inputStyle} />
            </div>

            {status === 'loading' && <LoadingState rows={6} />}
            {status === 'error' && <ErrorState error={error} onRetry={reload} />}
            {status === 'success' && content.length === 0 && hasFilters && (
              <NoResultsState
                onClear={() => {
                  setTargetType('');
                  setTargetId('');
                  setActorId('');
                }}
              />
            )}
            {status === 'success' && content.length === 0 && !hasFilters && (
              <EmptyState title="No audit events yet" icon="🧾" />
            )}
            {status === 'success' && content.length > 0 && (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Timestamp</th>
                      <th>Actor</th>
                      <th>Action</th>
                      <th>Target</th>
                      <th>Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {content.map((entry) => (
                      <tr key={entry.id}>
                        <td>{formatDateTime(entry.createdAt || entry.timestamp)}</td>
                        <td>{entry.actorName || entry.actorId}</td>
                        <td>{entry.action}</td>
                        <td>
                          {entry.targetType} #{entry.targetId}
                        </td>
                        <td>{entry.result || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {status === 'success' && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}
          </>
        )}
      </div>
    </div>
  );
}

const inputStyle = {
  padding: '8px 10px',
  borderRadius: 'var(--radius-sm)',
  border: '1px solid var(--border-strong)',
  background: 'var(--surface-2)',
  color: 'var(--text-primary)',
};
