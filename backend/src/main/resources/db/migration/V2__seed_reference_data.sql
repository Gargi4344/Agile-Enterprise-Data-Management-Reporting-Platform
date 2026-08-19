-- V2: Roles, permissions, role-permission grants, and demo accounts (synthetic, public-safe).
-- Demo passwords are documented in docs/demo-accounts.md and README; rotate before any non-demo use.

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Manages users, rules, and platform governance'),
    ('ANALYST', 'Inspects, cleans, and transforms datasets'),
    ('MANAGER', 'Operational visibility over datasets and reports'),
    ('OPERATOR', 'Handles failures, retries, and platform health');

INSERT INTO permissions (code, description) VALUES
    ('USER_MANAGE', 'Create, update, and deactivate platform users'),
    ('RULE_MANAGE_ALL', 'Create/update/deactivate any validation rule'),
    ('RULE_MANAGE_OWN', 'Create/update validation rules owned by the acting user'),
    ('DATASET_VIEW', 'View dataset registry, versions, and lineage'),
    ('DATASET_UPLOAD', 'Register datasets and upload new versions'),
    ('VALIDATION_RUN', 'Trigger validation runs against a dataset version'),
    ('PROCESS_APPROVE', 'Approve, reject, and process dataset versions'),
    ('REPORT_VIEW', 'View and export reports'),
    ('AUDIT_VIEW_ALL', 'View the full audit trail'),
    ('AUDIT_VIEW_LIMITED', 'View audit events scoped to owned datasets/reports');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE (r.name = 'ADMIN' AND p.code IN
        ('USER_MANAGE','RULE_MANAGE_ALL','DATASET_VIEW','DATASET_UPLOAD','VALIDATION_RUN','PROCESS_APPROVE','REPORT_VIEW','AUDIT_VIEW_ALL'))
   OR (r.name = 'ANALYST' AND p.code IN
        ('RULE_MANAGE_OWN','DATASET_VIEW','DATASET_UPLOAD','VALIDATION_RUN','PROCESS_APPROVE','REPORT_VIEW'))
   OR (r.name = 'MANAGER' AND p.code IN
        ('DATASET_VIEW','PROCESS_APPROVE','REPORT_VIEW','AUDIT_VIEW_LIMITED'))
   OR (r.name = 'OPERATOR' AND p.code IN
        ('DATASET_VIEW','DATASET_UPLOAD','VALIDATION_RUN','PROCESS_APPROVE','REPORT_VIEW','AUDIT_VIEW_ALL'));

-- Demo accounts. Passwords are synthetic and documented in docs/demo-accounts.md.
INSERT INTO users (email, password_hash, full_name, status) VALUES
    ('admin@demo.aedmp.local',    '$2b$10$PWX1pNLtWBq4skYgS8IbuOOuWhVbZ6PeOk5GlJTRbaSIKslWOY2X6', 'Ava Admin',      'ACTIVE'),
    ('analyst@demo.aedmp.local',  '$2b$10$tAPTuP/fYDIVoF.wzA50jOOvVUrYx.xSADD5UQChdBGKh5R7eFJdy', 'Alex Analyst',   'ACTIVE'),
    ('manager@demo.aedmp.local',  '$2b$10$7Hu32c59jQrPI0B4d5yc6O2hKQOCIGJK/8c9F/kHrSVeP/qA8TzBW', 'Morgan Manager', 'ACTIVE'),
    ('operator@demo.aedmp.local', '$2b$10$SQKHHsIfjbvhLnNtSNM36eKf/Gp/3QP.sIopKE.Aqbgorlzr6O07G', 'Oli Operator',   'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.email = 'admin@demo.aedmp.local' AND r.name = 'ADMIN')
   OR (u.email = 'analyst@demo.aedmp.local' AND r.name = 'ANALYST')
   OR (u.email = 'manager@demo.aedmp.local' AND r.name = 'MANAGER')
   OR (u.email = 'operator@demo.aedmp.local' AND r.name = 'OPERATOR');
