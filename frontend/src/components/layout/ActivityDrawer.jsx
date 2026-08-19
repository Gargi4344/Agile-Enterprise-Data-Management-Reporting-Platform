import { useAsync } from '../../hooks/useAsync.js';
import { auditApi } from '../../api/audit.js';
import { LoadingState } from '../states/LoadingState.jsx';
import { ErrorState } from '../states/ErrorState.jsx';
import { EmptyState } from '../states/EmptyState.jsx';
import { formatRelative } from '../../utils/format.js';
import { useAuth } from '../../context/AuthContext.jsx';

export function ActivityDrawer({ open, onClose }) {
  const { can } = useAuth();
  const allowed = can('AUDIT_VIEW_ALL') || can('AUDIT_VIEW_LIMITED');
  const { status, data, error, reload } = useAsync(
    () => (open && allowed ? auditApi.list({ page: 0, size: 10 }) : Promise.resolve(null)),
    [open, allowed]
  );

  return (
    <>
      <div className={`drawer-scrim${open ? ' open' : ''}`} onClick={onClose} aria-hidden={!open} />
      <aside className={`activity-drawer${open ? ' open' : ''}`} aria-label="Recent activity" aria-hidden={!open}>
        <div className="row-between" style={{ marginBottom: 12 }}>
          <h3>Recent activity</h3>
          <button type="button" className="btn btn-sm" onClick={onClose} aria-label="Close">
            Close
          </button>
        </div>
        {!allowed && <p className="text-secondary">You don't have permission to view the audit trail.</p>}
        {allowed && status === 'loading' && <LoadingState rows={5} />}
        {allowed && status === 'error' && <ErrorState error={error} onRetry={reload} />}
        {allowed && status === 'success' && (!data?.content || data.content.length === 0) && (
          <EmptyState title="No recent activity" icon="🕒" />
        )}
        {allowed && status === 'success' && data?.content?.length > 0 && (
          <ul className="activity-list">
            {data.content.map((entry) => (
              <li key={entry.id}>
                <div className="row-between">
                  <strong style={{ fontSize: '0.85rem' }}>{entry.action}</strong>
                  <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                    {formatRelative(entry.createdAt || entry.timestamp)}
                  </span>
                </div>
                <div className="text-secondary" style={{ fontSize: '0.8rem' }}>
                  {entry.actorName || entry.actorId} · {entry.targetType} {entry.targetId}
                </div>
              </li>
            ))}
          </ul>
        )}
      </aside>
    </>
  );
}
