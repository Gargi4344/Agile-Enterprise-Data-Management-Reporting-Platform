import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';

const NAV_ITEMS = [
  { to: '/overview', label: 'Overview' },
  { to: '/datasets', label: 'Datasets' },
  { to: '/validation', label: 'Validation' },
  { to: '/jobs', label: 'Jobs' },
  { to: '/reports', label: 'Reports' },
  { to: '/audit', label: 'Audit' },
  { to: '/admin', label: 'Admin', adminOnly: true },
];

export function TopNav({ onToggleActivity }) {
  const { user, logout, roles } = useAuth();
  const isAdmin = roles.includes('ADMIN');

  return (
    <header className="top-nav">
      <div className="top-nav-inner">
        <div className="top-nav-brand">
          <span className="top-nav-mark" aria-hidden="true">
            ▣
          </span>
          <span>AEDMRP</span>
        </div>
        <nav className="top-nav-links">
          {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `top-nav-link${isActive ? ' active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="top-nav-actions">
          <button type="button" className="btn btn-sm" onClick={onToggleActivity} aria-label="Recent activity">
            Activity
          </button>
          <div className="top-nav-user">
            <span className="top-nav-user-name">{user?.fullName}</span>
            <span className="text-muted" style={{ fontSize: '0.72rem' }}>
              {roles.join(', ')}
            </span>
          </div>
          <button type="button" className="btn btn-sm" onClick={logout}>
            Log out
          </button>
        </div>
      </div>
    </header>
  );
}
