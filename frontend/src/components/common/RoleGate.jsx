import { cloneElement, isValidElement } from 'react';
import { useAuth } from '../../context/AuthContext.jsx';

/**
 * Grays out (rather than hides) an action the current user's role can't perform, per the
 * RBAC matrix, with a tooltip explaining why — the server still enforces the real check.
 */
export function RoleGate({ permission, children, fallbackTitle }) {
  const { can } = useAuth();
  const allowed = can(permission);
  if (allowed) return children;
  const disabledChild = isValidElement(children)
    ? cloneElement(children, { disabled: true, 'aria-disabled': true })
    : children;
  return (
    <span title={fallbackTitle || "You don't have permission to do this."} style={{ display: 'inline-block' }}>
      {disabledChild}
    </span>
  );
}
