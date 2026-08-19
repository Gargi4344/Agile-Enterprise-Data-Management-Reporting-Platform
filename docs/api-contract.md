# API Contract — Agile Enterprise Data Management & Reporting Platform

This is the authoritative contract between `backend/` and `frontend/` for the first build pass.
Base URL in dev: `http://localhost:8080`. All endpoints except `/api/auth/login` and `/actuator/health`
require `Authorization: Bearer <jwt>`.

Full interactive docs are served at runtime from `/api/docs/ui` (springdoc/Swagger UI) once the
backend is implemented — treat this file as the pre-implementation source of truth and keep both
in sync as work proceeds.

## Conventions

- Paginated list endpoints accept `page` (0-based) and `size`, return:
  `{ content: [...], page, size, totalElements, totalPages }`
- Errors return: `{ code: "STABLE_ERROR_CODE", message: "human readable", requestId: "...", fieldErrors?: [{field,message}] }`
  with HTTP status 400 (validation), 401 (unauthenticated), 403 (forbidden), 404 (not found), 409 (conflict), 500 (server error).
- All mutating requests are logged to the audit trail (actor, action, target, result).
- Roles: `ADMIN`, `ANALYST`, `MANAGER`, `OPERATOR`. Permission matrix lives in `docs/rbac-matrix.md`.

## Auth

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/auth/login` | `{ email, password }` | Returns `{ token, expiresAt, user }` |
| GET | `/api/auth/me` | — | Current authenticated user + roles/permissions |

## Users / Roles / Permissions (Admin)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/users?page=&size=&search=` | List users |
| POST | `/api/users` | `{ email, fullName, password, roleNames: [] }` |
| PATCH | `/api/users/{id}` | `{ fullName?, status?, roleNames? }` |
| GET | `/api/roles` | List roles with permissions |
| GET | `/api/permissions` | List all permission codes |

## Datasets

| Method | Path | Notes |
|---|---|---|
| GET | `/api/datasets?page=&size=&search=` | Registry list |
| POST | `/api/datasets` | `{ name, description }` |
| GET | `/api/datasets/{id}` | Detail (includes latest version summary) |
| PATCH | `/api/datasets/{id}` | `{ name?, description?, status? }` |
| GET | `/api/datasets/{id}/versions` | Version history (immutable, newest first) |
| POST | `/api/datasets/{id}/versions` | multipart: `file`, `sourceSystem`; creates version + triggers schema profiling |
| GET | `/api/versions/{id}` | Version detail incl. lifecycle status |
| GET | `/api/versions/{id}/columns` | Profiled schema (expected vs observed) |

## Validation

| Method | Path | Notes |
|---|---|---|
| GET | `/api/validation/rules?datasetId=` | Rule catalog (global rules have `datasetId: null`) |
| POST | `/api/validation/rules` | Create rule |
| PATCH | `/api/validation/rules/{id}` | Update rule (analysts limited to rules they own) |
| POST | `/api/validation/runs` | `{ datasetVersionId }`; synchronous for v1, persists results |
| GET | `/api/validation/runs/{id}` | Run summary incl. quality score |
| GET | `/api/validation/runs?datasetVersionId=` | Run history for a version |
| GET | `/api/validation/runs/{id}/results?page=&size=&passed=&severity=` | Row/rule outcomes |

## Processing Jobs

| Method | Path | Notes |
|---|---|---|
| POST | `/api/jobs` | `{ datasetVersionId, jobType: TRANSFORMATION\|LOAD }`; idempotent per version+type. Requires the version to be `READY` (i.e. already validated). Validation itself is triggered via `/api/validation/runs`, not this endpoint — `jobType: VALIDATION` returns 400. |
| GET | `/api/jobs/{id}` | Job detail incl. retry count, timestamps |
| GET | `/api/jobs?datasetVersionId=&status=` | Job list / timeline |
| POST | `/api/jobs/{id}/retry` | Only valid from `FAILED` |

## Reports

| Method | Path | Notes |
|---|---|---|
| GET | `/api/reports?datasetId=` | Report metadata list |
| POST | `/api/reports` | `{ datasetVersionId, reportType }` |
| GET | `/api/reports/{id}` | Report detail incl. summary metrics |
| GET | `/api/reports/kpis/overview` | Operational KPIs for `/overview` dashboard |

## Audit

| Method | Path | Notes |
|---|---|---|
| GET | `/api/audit?targetType=&targetId=&actorId=&page=&size=` | Audit history (scope enforced server-side per role) |

## Health

| Method | Path | Notes |
|---|---|---|
| GET | `/actuator/health` | Liveness/readiness incl. DB check |

## Dataset version lifecycle (`status` field)

`UPLOADED → VALIDATING → READY|REJECTED → PROCESSING → COMPLETED|FAILED`, with `FAILED → RETRY|REVIEW`.
See `docs/state-machine.md` for the full transition table and idempotency rules.
