/**
 * Mirrors docs/rbac-matrix.md. The server is the enforcement authority (403s are always
 * respected); this map only drives client-side affordances (hiding/graying actions, choosing
 * between a permission-denied screen and a redirect loop) so the UI doesn't dead-end the user.
 */
export const PERMISSIONS_BY_ROLE = {
  ADMIN: ['USER_MANAGE', 'RULE_MANAGE_ALL', 'DATASET_VIEW', 'DATASET_UPLOAD', 'VALIDATION_RUN', 'PROCESS_APPROVE', 'REPORT_VIEW', 'AUDIT_VIEW_ALL'],
  ANALYST: ['RULE_MANAGE_OWN', 'DATASET_VIEW', 'DATASET_UPLOAD', 'VALIDATION_RUN', 'PROCESS_APPROVE', 'REPORT_VIEW'],
  MANAGER: ['DATASET_VIEW', 'PROCESS_APPROVE', 'REPORT_VIEW', 'AUDIT_VIEW_LIMITED'],
  OPERATOR: ['DATASET_VIEW', 'DATASET_UPLOAD', 'VALIDATION_RUN', 'PROCESS_APPROVE', 'REPORT_VIEW', 'AUDIT_VIEW_ALL'],
};

export function permissionsForRoles(roles = []) {
  const set = new Set();
  roles.forEach((role) => {
    (PERMISSIONS_BY_ROLE[role] || []).forEach((permission) => set.add(permission));
  });
  return set;
}

export function hasPermission(roles, permission) {
  return permissionsForRoles(roles).has(permission);
}
