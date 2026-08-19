-- V3: A single demo dataset with one version, profiled columns, and a starter rule catalog.
-- This gives a freshly-seeded environment something to show immediately; the full lifecycle
-- (new versions, validation runs, jobs, reports) is still exercised through the real API.

INSERT INTO datasets (name, description, owner_id, status)
SELECT 'Customer Orders (Demo)', 'Synthetic order extract used for the platform live demo.', u.id, 'ACTIVE'
FROM users u WHERE u.email = 'analyst@demo.aedmp.local';

INSERT INTO dataset_versions
    (dataset_id, version_number, source_filename, source_system, checksum_sha256, storage_location,
     file_size_bytes, row_count, status, uploaded_by, uploaded_at)
SELECT d.id, 1, 'customer_orders_seed_v1.csv', 'seed-data', 'seed0000000000000000000000000000000000000000000000000000000001',
       'local://seed-data/customer_orders_seed_v1.csv', 1024, 8, 'READY', u.id, now()
FROM datasets d, users u
WHERE d.name = 'Customer Orders (Demo)' AND u.email = 'analyst@demo.aedmp.local';

INSERT INTO dataset_columns
    (dataset_version_id, column_name, expected_type, observed_type, ordinal_position, nullable_observed, distinct_count, null_count, sample_values)
SELECT v.id, c.column_name, c.expected_type, c.observed_type, c.ordinal_position, c.nullable_observed, c.distinct_count, c.null_count, c.sample_values
FROM dataset_versions v
JOIN datasets d ON d.id = v.dataset_id AND d.name = 'Customer Orders (Demo)' AND v.version_number = 1
CROSS JOIN (VALUES
    ('order_id',       'STRING',  'STRING',  1, false, 8, 0, 'ORD-1001, ORD-1002, ORD-1003'),
    ('customer_email', 'STRING',  'STRING',  2, false, 8, 0, 'jane.doe@example.com, sam.lee@example.com'),
    ('order_date',     'DATE',    'DATE',    3, false, 6, 0, '2026-05-01, 2026-05-02'),
    ('amount',         'DECIMAL', 'DECIMAL', 4, false, 8, 0, '42.50, 108.00, 19.99'),
    ('status',         'STRING',  'STRING',  5, false, 3, 0, 'PENDING, SHIPPED, DELIVERED'),
    ('region',         'STRING',  'STRING',  6, true,  4, 1, 'NA, EMEA, APAC')
) AS c(column_name, expected_type, observed_type, ordinal_position, nullable_observed, distinct_count, null_count, sample_values);

INSERT INTO validation_rules
    (name, description, rule_family, severity, dataset_id, column_name, expression_type, expression, active, weight, owner_id, version)
SELECT r.name, r.description, r.rule_family, r.severity, d.id, r.column_name, r.expression_type, r.expression, true, r.weight, u.id, 1
FROM datasets d, users u
CROSS JOIN (VALUES
    ('Order ID required',        'order_id must not be missing',              'COMPLETENESS', 'BLOCKER', 'order_id',       'NOT_NULL',       'order_id IS NOT NULL', 1.0),
    ('Order ID unique',          'order_id must be unique within the version', 'UNIQUENESS',   'BLOCKER', 'order_id',       'UNIQUE',         'UNIQUE(order_id)', 1.0),
    ('Customer email required',  'customer_email must not be missing',        'COMPLETENESS', 'BLOCKER', 'customer_email', 'NOT_NULL',       'customer_email IS NOT NULL', 1.0),
    ('Customer email format',    'customer_email must look like an email',    'VALIDITY',      'WARNING', 'customer_email', 'REGEX',          '^[^@\s]+@[^@\s]+\.[^@\s]+$', 0.5),
    ('Amount non-negative',      'amount must be zero or greater',            'VALIDITY',      'BLOCKER', 'amount',         'RANGE_MIN',      '0', 1.0),
    ('Status in allowed set',    'status must be one of the known values',    'VALIDITY',      'BLOCKER', 'status',         'ENUM',           'PENDING,SHIPPED,DELIVERED,CANCELLED', 1.0)
) AS r(name, description, rule_family, severity, column_name, expression_type, expression, weight)
WHERE d.name = 'Customer Orders (Demo)' AND u.email = 'analyst@demo.aedmp.local';
