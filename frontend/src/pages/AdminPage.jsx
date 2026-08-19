import { useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { UsersAdminPanel } from '../components/admin/UsersAdminPanel.jsx';
import { RolesAdminPanel } from '../components/admin/RolesAdminPanel.jsx';
import { RuleCatalog } from '../components/validation/RuleCatalog.jsx';

const TABS = [
  { key: 'users', label: 'Users' },
  { key: 'roles', label: 'Roles & permissions' },
  { key: 'rules', label: 'Global rules' },
];

export function AdminPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState('users');

  if (!can('USER_MANAGE')) {
    return (
      <div className="page">
        <PermissionDenied message="Admin console is restricted to the ADMIN role." />
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Admin console</h1>
          <p className="page-subtitle">Manage users, roles, and global validation rules.</p>
        </div>
      </div>

      <div className="tabs" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            className={`tab-button${tab === t.key ? ' active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="card">
        {tab === 'users' && <UsersAdminPanel />}
        {tab === 'roles' && <RolesAdminPanel />}
        {tab === 'rules' && <RuleCatalog datasetId={undefined} />}
      </div>
    </div>
  );
}
