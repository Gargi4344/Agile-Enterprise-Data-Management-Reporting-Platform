# RBAC Matrix

Permissions are granted to roles via `role_permissions` (seeded in `V2__seed_reference_data.sql`).
A user's effective permissions are the union of permissions across all roles assigned to them.

| Permission code | Admin | Analyst | Manager | Operator | Meaning |
|---|---|---|---|---|---|
| `USER_MANAGE` | ✓ | — | — | — | Create/update/deactivate users |
| `RULE_MANAGE_ALL` | ✓ | — | — | — | Manage any validation rule |
| `RULE_MANAGE_OWN` | — | ✓ | — | — | Manage validation rules the user owns |
| `DATASET_VIEW` | ✓ | ✓ | ✓ | ✓ | View dataset registry/lineage |
| `DATASET_UPLOAD` | ✓ | ✓ | — | ✓ | Register datasets / upload versions |
| `VALIDATION_RUN` | ✓ | ✓ | — | ✓ | Trigger validation runs |
| `PROCESS_APPROVE` | ✓ | ✓ | ✓ | ✓ | Approve/reject/process a version, retry jobs |
| `REPORT_VIEW` | ✓ | ✓ | ✓ | ✓ | View/export reports |
| `AUDIT_VIEW_ALL` | ✓ | — | — | ✓ | View full audit trail |
| `AUDIT_VIEW_LIMITED` | — | — | ✓ | — | View audit events for owned datasets/reports only |

Enforcement is server-side on every endpoint (Spring Security method security via `@PreAuthorize`
against permission codes, not role names, so the mapping above is the single source of truth).
Unauthorized requests return 401 (no/invalid token) or 403 (authenticated, insufficient permission) —
never a 200 with filtered data and never a stack trace.
