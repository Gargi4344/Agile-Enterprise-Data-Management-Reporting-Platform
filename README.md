# Agile Enterprise Data Management & Reporting Platform

A governed dataset lifecycle platform: register a dataset, upload a version, profile its schema,
run it through a configurable data-quality rule engine, process it (transform + load), generate a
report, and see every step in an audit trail. Built as a portfolio project against the master spec
in this repo's planning docs — see [`docs/`](docs/) for the API contract, RBAC matrix, and state
machine that the implementation follows exactly.

> **Status:** Backend (Spring Boot) and frontend (React) are implemented and tested locally.
> AWS deployment, CI/CD wiring, and load testing described in the master spec have **not** been
> executed yet — see [Known limitations](#known-limitations) for exactly what is and isn't verified.
> Do not treat any number in this README as a claim beyond what's stated as measured below.

## Problem

Teams that receive external data extracts need more than an upload form: they need to know who
owns a dataset, whether a new file actually passed quality checks, what changed between versions,
and who did what and when. This platform models that as an explicit lifecycle instead of a bag of
CRUD screens.

## Solution

- **Dataset registry & versioning** — datasets are immutable-history version chains (`datasets` →
  `dataset_versions`), never silently overwritten.
- **Schema profiling** — every uploaded CSV is profiled on upload: inferred type, null/distinct
  counts, and sample values per column, stored in `dataset_columns`.
- **Quality engine** — a rule catalog (`validation_rules`) evaluated by a deterministic rule engine
  against every row, persisting failures to `validation_results` and a run summary with a quality
  score to `validation_runs`.
- **Processing jobs** — transform + load work is tracked as `processing_jobs` with retry, an
  idempotency key per (version, job type), and a small deterministic transformation executor.
- **Reporting** — per-version reports with summary metrics, plus an operational KPI overview
  endpoint for the dashboard.
- **RBAC + audit** — four roles (Admin/Analyst/Manager/Operator) mapped to permission codes
  enforced server-side on every endpoint; every mutating action writes an `audit_logs` row.

## Lifecycle

```
UPLOAD → PROFILE → VALIDATE → (READY | REJECTED) → PROCESS → (COMPLETED | FAILED → RETRY)
```

The full state machine (dataset version status + processing job status, allowed transitions, and
idempotency rules) is documented in [`docs/state-machine.md`](docs/state-machine.md) and enforced
in code by `DatasetVersionStatus`/`JobStatus` (each transition is an explicit allow-list — an
invalid transition throws a 409, it can't silently happen).

## Architecture

```
React (Vite, plain JS)  ──HTTP/JSON, JWT bearer──▶  Spring Boot 3 / Java 21
        │                                                    │
        │                                          Spring Security (permission-
        │                                          code based @PreAuthorize)
        │                                                    │
   nginx (prod)                                    Flyway-migrated PostgreSQL
                                                     Local disk or private S3
                                                     (FileStorageService)
```

- **Backend**: `backend/` — layered by domain (`dataset`, `validation`, `job`, `report`, `audit`,
  `user`, `auth`, `security`, `config`, `common`). See [`docs/api-contract.md`](docs/api-contract.md)
  for the full endpoint list (31 REST endpoints across auth, users/roles/permissions, datasets,
  versions, columns, validation rules/runs/results, jobs, reports, audit, and health).
- **Frontend**: `frontend/` — React + Vite, no TypeScript and no heavy UI kit (per the spec's
  "don't add unnecessary technologies" rule), plain CSS. Routes: `/login`, `/overview`, `/datasets`,
  `/datasets/:id`, `/validation`, `/jobs`, `/reports`, `/admin`, `/audit`.
- **Storage**: `FileStorageService` is an interface with a `LocalFileStorageService` (dev/CI) and an
  `S3FileStorageService` (private-bucket, AWS mode) implementation selected by
  `aedmp.storage.mode`.

## ERD (core tables)

15 tables created in [`V1__init_schema.sql`](backend/src/main/resources/db/migration/V1__init_schema.sql):
`users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `datasets`, `dataset_versions`,
`dataset_columns`, `validation_rules`, `validation_runs`, `validation_results`, `processing_jobs`,
`transformation_rules`, `reports`, `audit_logs`.

```
datasets 1─* dataset_versions 1─* dataset_columns
dataset_versions 1─* validation_runs 1─* validation_results *─1 validation_rules
dataset_versions 1─* processing_jobs
dataset_versions 1─* reports
users *─* roles *─* permissions
(almost) every table → audit_logs, linked loosely by target_type/target_id, not FK
```

## API docs

Interactive OpenAPI/Swagger UI is served at runtime from `/api/docs/ui` (springdoc) once the
backend is running. [`docs/api-contract.md`](docs/api-contract.md) is the pre-implementation
contract the frontend was built against and stays the human-readable reference.

## Quality engine

Rule families: `SCHEMA`, `COMPLETENESS`, `VALIDITY`, `UNIQUENESS`, `CONSISTENCY`, `REFERENTIAL`,
`BUSINESS`. Each rule has a severity (`BLOCKER`/`WARNING`) and an expression type (`NOT_NULL`,
`UNIQUE`, `REGEX`, `RANGE_MIN`, `RANGE_MAX`, `ENUM`, `TYPE_MATCH`, `COLUMN_PRESENT`) evaluated by
`RuleEngine` (`backend/src/main/java/com/aedmp/platform/validation/engine/RuleEngine.java`) — a
pure function over a parsed CSV table, unit-tested directly (no DB/Spring context needed).

**Quality score** = `accepted_records / total_records × 100`, where a record is accepted iff it has
zero `BLOCKER`-severity failures for that run. Computed and persisted per validation run
(`validation_runs.quality_score`); see [`docs/state-machine.md`](docs/state-machine.md) for the
exact definition and the weighting caveat.

## Local setup

Prerequisites: JDK 21+, Maven, Node 20+, Docker (for Postgres and for the Testcontainers-backed
integration test), and a Docker daemon reachable from your shell.

```bash
# 1. Start Postgres (adjust to however you run it locally, e.g. docker run or docker-compose)
docker run -d --name aedmp-db -p 5432:5432 \
  -e POSTGRES_DB=aedmp -e POSTGRES_USER=aedmp_app -e POSTGRES_PASSWORD=aedmp_app_password \
  postgres:16-alpine

# 2. Backend (runs Flyway migrations automatically on startup)
cd backend
mvn spring-boot:run
# API on http://localhost:8080, Swagger UI on http://localhost:8080/api/docs/ui

# 3. Frontend
cd frontend
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev
# App on http://localhost:5173 (proxies /api and /actuator to :8080 in dev)
```

### Demo accounts

Seeded by `V2__seed_reference_data.sql` (synthetic, public-safe — see
[`docs/rbac-matrix.md`](docs/rbac-matrix.md) for what each role can do):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@demo.aedmp.local` | `DemoAdmin!2026` |
| Analyst | `analyst@demo.aedmp.local` | `DemoAnalyst!2026` |
| Manager | `manager@demo.aedmp.local` | `DemoManager!2026` |
| Operator | `operator@demo.aedmp.local` | `DemoOperator!2026` |

Rotate these before using the platform for anything beyond local demo/dev.

## Seed data

`V3__seed_demo_dataset.sql` registers one demo dataset ("Customer Orders (Demo)") with a profiled
version and a starter rule catalog, so a fresh environment isn't empty on first load.
[`seed-data/`](seed-data/) also has fixture CSVs for the critical test scenarios from the master
spec: a clean file, one missing a required column, one with bad types/duplicates, and an empty file
(header only) — used by the integration tests and available for manual demo walkthroughs.

## Tests

- **Backend**: 38 tests, all passing — 28 pure-logic unit tests (rule engine, schema type
  inference, dataset-version/job state machines, transformation executor — no Spring context) plus
  a 10-scenario `@SpringBootTest` integration suite (`PlatformIntegrationTest`) run against a real
  Postgres via Testcontainers, covering: login success/failure, unauthenticated (401) vs
  unauthorized (403), the full upload → reject → fix → validate → process → report → audit
  lifecycle, duplicate-checksum rejection, job idempotency, "job before version is ready" rejection,
  missing-required-column schema failure, empty-dataset validation, and failed-job retry.
  Run with `cd backend && mvn test` (needs Docker; see note below).
- **Frontend**: 21 tests passing (Vitest + React Testing Library) — login form validation, the
  lifecycle stepper's per-status rendering, shared UI-state components, and the RBAC utility. Run
  with `cd frontend && npm test`.
- **Not yet built**: dedicated `e2e/`, `load-tests/` suites and CI wiring are placeholders in the
  repo layout but not implemented — see Known limitations.

> **Docker on Windows note:** if the integration test can't find a Docker environment, export
> `DOCKER_HOST` explicitly to your active Docker context's pipe/socket before running `mvn test`
> (check with `docker context inspect`) — Testcontainers' auto-discovery didn't work out of the box
> in this project's dev environment even though the `docker` CLI worked fine in the same shell.

## Coverage

JaCoCo instruction coverage after `mvn test`: **72%** (4,551 / 6,320 instructions), report at
`backend/target/site/jacoco/index.html`. This is a real measured number, not a target — it is
below the master spec's aspirational 90%+ and is called out as such rather than rounded up.

## Benchmark

Not yet run. The master spec calls for defined-load p50/p95/p99 API latency measurements and a
processing throughput benchmark under `load-tests/`; neither has been executed against this build,
so no latency or throughput numbers are claimed here.

## AWS

Not yet deployed. `S3FileStorageService` implements the private-S3 storage path described in the
spec (SSE-AES256, no public ACLs) and is selected via `aedmp.storage.mode=s3`, but it has not been
exercised against a real AWS account — it compiles and is unit-reachable but is unverified in a
live environment. No ECS/RDS/CloudFront/Secrets Manager resources have been provisioned. Terraform/
CDK definitions for the target architecture (`infra/`) have not been written yet.

## Security

- Passwords hashed with BCrypt (Spring Security `BCryptPasswordEncoder`).
- Stateless JWT auth (HMAC-signed, `jjwt`), no server-side session.
- Authorization enforced server-side per endpoint via `@PreAuthorize` against **permission codes**
  (not role names) — see [`docs/rbac-matrix.md`](docs/rbac-matrix.md) for the single source of
  truth mapping roles → permissions.
- Distinct 401 (unauthenticated) vs 403 (authenticated, insufficient permission) responses, both
  JSON error envelopes, never a stack trace or silently-filtered 200.
- File upload size limits (25MB, `spring.servlet.multipart.max-file-size`), CSV parsing rejects
  malformed rows rather than guessing.
- Audit log records actor, action, target, result, request ID, and safe metadata — never
  passwords/secrets (see `AuditService`).
- No real customer PII anywhere in the repo — all seed/demo data is synthetic.
- **Not yet done**: dependency vulnerability scanning, TLS termination config, rate/size limiting
  beyond the multipart cap, and a security review pass are not part of this build yet.

## Screenshots

Not captured yet — pending a manual walkthrough of the running app. Will be added under `docs/` once
captured; no screenshots are included in this commit.

## Known limitations

Being explicit here rather than letting these surface as surprises, per the "do not invent
implementation evidence" rule this project follows:

- **AWS deployment**: not done. Storage abstraction supports S3 in code; nothing has been deployed.
- **CI/CD**: `.github/workflows/` is empty; tests are run locally only so far.
- **Docker Compose**: not yet added at the repo root (local setup above uses a plain `docker run`
  for Postgres); `backend/Dockerfile` and `frontend/Dockerfile` exist and build the two services
  independently but haven't been wired together with a compose file yet.
- **Load/perf testing**: not run; no latency or throughput claims are made.
- **E2E UI tests**: `e2e/` is an empty placeholder; UI flows are covered by frontend unit tests and
  the backend's HTTP-level integration test, not a browser-driven E2E suite.
- **Audit scope for the Manager role**: `AUDIT_VIEW_LIMITED` is implemented as "the manager's own
  actions only" rather than "actions on datasets/reports they own" — a documented simplification
  (see `AuditController`'s class doc comment) rather than the fuller ownership-join the spec's
  wording could imply.
- **Frontend gaps** (from the build agent's own report): rule editing (`PATCH
  /api/validation/rules/{id}`) isn't wired to any UI action yet (create + list only); the profiled
  schema-columns endpoint isn't surfaced on the dataset detail page; `GET /api/auth/me` isn't
  called on app load (the app trusts the cached login response and relies on a global 401 handler
  for expired tokens).

## Future work

- Docker Compose for one-command local stack (Postgres + backend + frontend).
- GitHub Actions CI: build, test, lint, Docker image build, smoke test.
- Terraform/CDK for the AWS target architecture (ECS/Fargate, RDS, S3, CloudFront, Secrets Manager).
- Load testing (k6 or similar) against `load-tests/` for the p50/p95/p99 and throughput claims.
- E2E browser tests for the demo sequence (login → upload → validate → process → report → audit).
- Wire rule editing and schema-columns view into the frontend; call `/api/auth/me` on load.
