import { useState } from 'react';
import { usePaginatedFetch } from '../../hooks/usePaginatedFetch.js';
import { usersApi } from '../../api/users.js';
import { LoadingState } from '../states/LoadingState.jsx';
import { ErrorState, ValidationErrorBanner } from '../states/ErrorState.jsx';
import { EmptyState, NoResultsState } from '../states/EmptyState.jsx';
import { Pagination } from '../common/Pagination.jsx';
import { Modal } from '../common/Modal.jsx';
import { StatusBadge } from '../common/StatusBadge.jsx';

const ALL_ROLES = ['ADMIN', 'ANALYST', 'MANAGER', 'OPERATOR'];

export function UsersAdminPanel() {
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);

  const { content, status, error, page, setPage, totalPages, reload } = usePaginatedFetch(
    ({ page: p, size, search: s }) => usersApi.list({ page: p, size, search: s }),
    { search }
  );

  return (
    <div>
      <div className="row-between" style={{ marginBottom: 14, flexWrap: 'wrap', gap: 10 }}>
        <input
          type="search"
          placeholder="Search users…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ padding: '8px 10px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-strong)', background: 'var(--surface-2)', color: 'var(--text-primary)', flex: 1, minWidth: 200 }}
        />
        <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
          + Add user
        </button>
      </div>

      {status === 'loading' && <LoadingState rows={4} />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}
      {status === 'success' && content.length === 0 && search && <NoResultsState query={search} onClear={() => setSearch('')} />}
      {status === 'success' && content.length === 0 && !search && <EmptyState title="No users yet" icon="👤" />}
      {status === 'success' && content.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {content.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{(u.roleNames || u.roles || []).join(', ')}</td>
                  <td>{u.status ? <StatusBadge status={u.status} /> : '—'}</td>
                  <td>
                    <button type="button" className="btn btn-sm" onClick={() => setEditUser(u)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {status === 'success' && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}

      {createOpen && (
        <UserFormModal
          title="Add user"
          onClose={() => setCreateOpen(false)}
          onSubmit={async (payload) => usersApi.create(payload)}
          onDone={() => {
            setCreateOpen(false);
            reload();
          }}
          withPassword
        />
      )}
      {editUser && (
        <UserFormModal
          title={`Edit ${editUser.fullName}`}
          initial={editUser}
          onClose={() => setEditUser(null)}
          onSubmit={async (payload) => usersApi.update(editUser.id, payload)}
          onDone={() => {
            setEditUser(null);
            reload();
          }}
        />
      )}
    </div>
  );
}

function UserFormModal({ title, initial, onClose, onSubmit, onDone, withPassword }) {
  const [email, setEmail] = useState(initial?.email || '');
  const [fullName, setFullName] = useState(initial?.fullName || '');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState(initial?.status || 'ACTIVE');
  const [roleNames, setRoleNames] = useState(initial?.roleNames || initial?.roles || []);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  function toggleRole(role) {
    setRoleNames((prev) => (prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const payload = withPassword ? { email, fullName, password, roleNames } : { fullName, status, roleNames };
      await onSubmit(payload);
      onDone();
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        {withPassword && (
          <div className="field">
            <label htmlFor="user-email">Email</label>
            <input id="user-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
        )}
        <div className="field">
          <label htmlFor="user-name">Full name</label>
          <input id="user-name" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </div>
        {withPassword && (
          <div className="field">
            <label htmlFor="user-password">Temporary password</label>
            <input id="user-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
        )}
        {!withPassword && (
          <div className="field">
            <label htmlFor="user-status">Status</label>
            <select id="user-status" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        )}
        <div className="field">
          <label>Roles</label>
          <div className="stack">
            {ALL_ROLES.map((role) => (
              <label key={role} className="row" style={{ fontSize: '0.85rem', fontWeight: 400 }}>
                <input type="checkbox" checked={roleNames.includes(role)} onChange={() => toggleRole(role)} />
                {role}
              </label>
            ))}
          </div>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
