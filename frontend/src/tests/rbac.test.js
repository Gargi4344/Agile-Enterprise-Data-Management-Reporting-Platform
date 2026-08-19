import { describe, it, expect } from 'vitest';
import { hasPermission, permissionsForRoles } from '../utils/rbac.js';

describe('rbac utility (mirrors docs/rbac-matrix.md)', () => {
  it('grants ADMIN the union of admin-only and shared permissions', () => {
    expect(hasPermission(['ADMIN'], 'USER_MANAGE')).toBe(true);
    expect(hasPermission(['ADMIN'], 'RULE_MANAGE_ALL')).toBe(true);
    expect(hasPermission(['ADMIN'], 'AUDIT_VIEW_ALL')).toBe(true);
  });

  it('denies MANAGER dataset-mutation permissions it does not hold', () => {
    expect(hasPermission(['MANAGER'], 'DATASET_UPLOAD')).toBe(false);
    expect(hasPermission(['MANAGER'], 'VALIDATION_RUN')).toBe(false);
    expect(hasPermission(['MANAGER'], 'PROCESS_APPROVE')).toBe(true);
    expect(hasPermission(['MANAGER'], 'AUDIT_VIEW_LIMITED')).toBe(true);
  });

  it('denies ANALYST audit access entirely', () => {
    expect(hasPermission(['ANALYST'], 'AUDIT_VIEW_ALL')).toBe(false);
    expect(hasPermission(['ANALYST'], 'AUDIT_VIEW_LIMITED')).toBe(false);
  });

  it('unions permissions across multiple assigned roles', () => {
    const perms = permissionsForRoles(['MANAGER', 'OPERATOR']);
    expect(perms.has('AUDIT_VIEW_LIMITED')).toBe(true);
    expect(perms.has('AUDIT_VIEW_ALL')).toBe(true);
    expect(perms.has('DATASET_UPLOAD')).toBe(true);
  });

  it('returns no permissions for an unknown or empty role list', () => {
    expect(permissionsForRoles([]).size).toBe(0);
    expect(permissionsForRoles(['NOT_A_ROLE']).size).toBe(0);
  });
});
