-- Agile Enterprise Data Management & Reporting Platform
-- V1: Core schema - identity, governance, dataset lifecycle, quality, jobs, reporting, audit

CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(40) NOT NULL UNIQUE,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(60) NOT NULL UNIQUE,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE datasets (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    owner_id        BIGINT NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dataset_name_owner UNIQUE (name, owner_id)
);

CREATE TABLE dataset_versions (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    version_number      INTEGER NOT NULL,
    source_filename     VARCHAR(500) NOT NULL,
    source_system       VARCHAR(120) NOT NULL DEFAULT 'manual-upload',
    checksum_sha256     VARCHAR(64) NOT NULL,
    storage_location    VARCHAR(1000) NOT NULL,
    file_size_bytes     BIGINT NOT NULL DEFAULT 0,
    row_count           BIGINT,
    status              VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
                            CHECK (status IN ('UPLOADED','VALIDATING','READY','PROCESSING','REJECTED','FAILED','COMPLETED')),
    uploaded_by         BIGINT NOT NULL REFERENCES users(id),
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dataset_version UNIQUE (dataset_id, version_number),
    CONSTRAINT uq_dataset_checksum UNIQUE (dataset_id, checksum_sha256)
);

CREATE TABLE dataset_columns (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_version_id  BIGINT NOT NULL REFERENCES dataset_versions(id) ON DELETE CASCADE,
    column_name         VARCHAR(255) NOT NULL,
    expected_type       VARCHAR(40),
    observed_type       VARCHAR(40) NOT NULL,
    ordinal_position    INTEGER NOT NULL,
    nullable_observed   BOOLEAN NOT NULL DEFAULT true,
    distinct_count      BIGINT,
    null_count          BIGINT,
    sample_values       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_version_column UNIQUE (dataset_version_id, column_name)
);

CREATE TABLE validation_rules (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    rule_family     VARCHAR(20) NOT NULL
                        CHECK (rule_family IN ('SCHEMA','COMPLETENESS','VALIDITY','UNIQUENESS','CONSISTENCY','REFERENTIAL','BUSINESS')),
    severity        VARCHAR(20) NOT NULL DEFAULT 'BLOCKER' CHECK (severity IN ('BLOCKER','WARNING')),
    dataset_id      BIGINT REFERENCES datasets(id) ON DELETE CASCADE,
    column_name     VARCHAR(255),
    expression_type VARCHAR(30) NOT NULL,
    expression      VARCHAR(2000) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    weight          NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    owner_id        BIGINT NOT NULL REFERENCES users(id),
    version         INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE validation_runs (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_version_id  BIGINT NOT NULL REFERENCES dataset_versions(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                            CHECK (status IN ('RUNNING','PASSED','FAILED','PASSED_WITH_WARNINGS')),
    triggered_by        BIGINT NOT NULL REFERENCES users(id),
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    total_records       BIGINT NOT NULL DEFAULT 0,
    accepted_records     BIGINT NOT NULL DEFAULT 0,
    rejected_records    BIGINT NOT NULL DEFAULT 0,
    quality_score       NUMERIC(5,2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE validation_results (
    id                  BIGSERIAL PRIMARY KEY,
    validation_run_id   BIGINT NOT NULL REFERENCES validation_runs(id) ON DELETE CASCADE,
    rule_id             BIGINT REFERENCES validation_rules(id),
    row_number          BIGINT,
    column_name         VARCHAR(255),
    severity            VARCHAR(20) NOT NULL CHECK (severity IN ('BLOCKER','WARNING')),
    passed              BOOLEAN NOT NULL,
    message             VARCHAR(1000) NOT NULL,
    sample_value        VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transformation_rules (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(2000),
    dataset_id          BIGINT REFERENCES datasets(id) ON DELETE CASCADE,
    transformation_type VARCHAR(30) NOT NULL,
    expression          VARCHAR(2000) NOT NULL,
    ordinal_position    INTEGER NOT NULL DEFAULT 0,
    version             INTEGER NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE processing_jobs (
    id                      BIGSERIAL PRIMARY KEY,
    dataset_version_id      BIGINT NOT NULL REFERENCES dataset_versions(id) ON DELETE CASCADE,
    job_type                VARCHAR(20) NOT NULL CHECK (job_type IN ('VALIDATION','TRANSFORMATION','LOAD')),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','RETRYING')),
    idempotency_key         VARCHAR(255) NOT NULL,
    retry_count             INTEGER NOT NULL DEFAULT 0,
    max_retries             INTEGER NOT NULL DEFAULT 3,
    error_message           VARCHAR(2000),
    transformation_version  INTEGER,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_by              BIGINT NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE reports (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    dataset_id          BIGINT REFERENCES datasets(id) ON DELETE CASCADE,
    dataset_version_id  BIGINT REFERENCES dataset_versions(id) ON DELETE CASCADE,
    report_type         VARCHAR(30) NOT NULL DEFAULT 'DATASET_QUALITY',
    status               VARCHAR(20) NOT NULL DEFAULT 'GENERATED' CHECK (status IN ('GENERATED','FAILED')),
    generated_by        BIGINT NOT NULL REFERENCES users(id),
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    parameters          JSONB NOT NULL DEFAULT '{}'::jsonb,
    summary_metrics     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        BIGINT REFERENCES users(id),
    actor_email     VARCHAR(255) NOT NULL,
    action          VARCHAR(80) NOT NULL,
    target_type     VARCHAR(60) NOT NULL,
    target_id       VARCHAR(60),
    request_id      VARCHAR(80),
    job_id          BIGINT,
    result          VARCHAR(20) NOT NULL CHECK (result IN ('SUCCESS','FAILURE','DENIED')),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dataset_versions_dataset ON dataset_versions(dataset_id);
CREATE INDEX idx_dataset_columns_version ON dataset_columns(dataset_version_id);
CREATE INDEX idx_validation_rules_dataset ON validation_rules(dataset_id);
CREATE INDEX idx_validation_runs_version ON validation_runs(dataset_version_id);
CREATE INDEX idx_validation_results_run ON validation_results(validation_run_id);
CREATE INDEX idx_validation_results_rule ON validation_results(rule_id);
CREATE INDEX idx_processing_jobs_version ON processing_jobs(dataset_version_id);
CREATE INDEX idx_processing_jobs_status ON processing_jobs(status);
CREATE INDEX idx_reports_dataset ON reports(dataset_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_datasets_owner ON datasets(owner_id);
