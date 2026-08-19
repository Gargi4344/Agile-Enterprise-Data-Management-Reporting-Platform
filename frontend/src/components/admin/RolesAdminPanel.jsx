import { useAsync } from '../../hooks/useAsync.js';
import { usersApi } from '../../api/users.js';
import { LoadingState } from '../states/LoadingState.jsx';
import { ErrorState } from '../states/ErrorState.jsx';
import { EmptyState } from '../states/EmptyState.jsx';

export function RolesAdminPanel() {
  const rolesQuery = useAsync(() => usersApi.roles(), []);

  if (rolesQuery.status === 'loading') return <LoadingState rows={3} />;
  if (rolesQuery.status === 'error') return <ErrorState error={rolesQuery.error} onRetry={rolesQuery.reload} />;
  const roles = rolesQuery.data || [];
  if (roles.length === 0) return <EmptyState title="No roles found" icon="🛡️" />;

  return (
    <div className="stack" style={{ gap: 16 }}>
      {roles.map((role) => (
        <div key={role.name || role.id} className="card" style={{ boxShadow: 'none' }}>
          <h3 style={{ fontSize: '0.95rem', marginBottom: 8 }}>{role.name}</h3>
          <div className="row" style={{ flexWrap: 'wrap', gap: 6 }}>
            {(role.permissions || []).map((perm) => (
              <span key={perm} className="badge badge-neutral">
                <span className="badge-dot" aria-hidden="true" />
                {perm}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
