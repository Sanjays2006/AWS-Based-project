-- ============================================================
-- V3: IAM Identities, Activity Logs, Audit Logs
-- ============================================================

CREATE TABLE iam_identities (
    id              BIGSERIAL    PRIMARY KEY,
    arn             VARCHAR(500) NOT NULL UNIQUE,
    identity_type   VARCHAR(50),
    account_id      VARCHAR(20),
    identity_name   VARCHAR(200),
    region          VARCHAR(50),
    is_monitored    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activity_logs (
    id                  BIGSERIAL    PRIMARY KEY,
    iam_identity_id     BIGINT       REFERENCES iam_identities(id) ON DELETE SET NULL,
    user_id             BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    event_name          VARCHAR(200),
    event_source        VARCHAR(200),
    event_time          TIMESTAMP,
    source_ip           VARCHAR(50),
    user_agent          VARCHAR(500),
    region              VARCHAR(50),
    aws_service         VARCHAR(100),
    request_parameters  JSONB,
    response_elements   JSONB,
    raw_event           JSONB,
    data_volume_bytes   BIGINT       NOT NULL DEFAULT 0,
    ingested_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(200),
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    description TEXT,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(300),
    old_values  JSONB,
    new_values  JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_identities_arn           ON iam_identities(arn);
CREATE INDEX idx_iam_identities_monitored     ON iam_identities(is_monitored);
CREATE INDEX idx_activity_logs_identity       ON activity_logs(iam_identity_id, event_time DESC);
CREATE INDEX idx_activity_logs_user           ON activity_logs(user_id, event_time DESC);
CREATE INDEX idx_activity_logs_event_time     ON activity_logs(event_time DESC);
CREATE INDEX idx_activity_logs_event_name     ON activity_logs(event_name);
CREATE INDEX idx_activity_logs_aws_service    ON activity_logs(aws_service);
CREATE INDEX idx_audit_logs_user              ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity            ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at        ON audit_logs(created_at DESC);
