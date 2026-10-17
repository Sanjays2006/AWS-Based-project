-- Flyway Migration V11: Identity Audit Versioning & IAM Policy Analyzer tables

CREATE TABLE IF NOT EXISTS iam_identity_versions (
    id BIGSERIAL PRIMARY KEY,
    iam_identity_id BIGINT NOT NULL REFERENCES iam_identities(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    identity_name VARCHAR(128) NOT NULL,
    attached_policies TEXT,
    risk_weight_modifier DOUBLE PRECISION DEFAULT 1.0,
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_iam_identity_versions_identity ON iam_identity_versions(iam_identity_id, version_number DESC);
