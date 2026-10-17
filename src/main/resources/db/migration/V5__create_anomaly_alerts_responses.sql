-- ============================================================
-- V5: Anomaly Events, Alert Notifications, Automated Responses,
--     Report History, System Settings
-- ============================================================

CREATE TABLE anomaly_events (
    id                  BIGSERIAL       PRIMARY KEY,
    iam_identity_id     BIGINT          NOT NULL REFERENCES iam_identities(id) ON DELETE CASCADE,
    activity_log_id     BIGINT          REFERENCES activity_logs(id) ON DELETE SET NULL,
    anomaly_type        VARCHAR(100)    NOT NULL,
    severity            VARCHAR(20)     NOT NULL,
    description         TEXT,
    confidence_score    DECIMAL(5,2),
    risk_delta          DECIMAL(5,2)    DEFAULT 0,
    is_acknowledged     BOOLEAN         NOT NULL DEFAULT FALSE,
    acknowledged_by     BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at     TIMESTAMP,
    alert_response      VARCHAR(20),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alert_notifications (
    id                  BIGSERIAL    PRIMARY KEY,
    anomaly_event_id    BIGINT       REFERENCES anomaly_events(id) ON DELETE SET NULL,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel             VARCHAR(50)  NOT NULL DEFAULT 'IN_APP',
    title               VARCHAR(300) NOT NULL,
    message             TEXT,
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at             TIMESTAMP,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE automated_responses (
    id                  BIGSERIAL    PRIMARY KEY,
    anomaly_event_id    BIGINT       REFERENCES anomaly_events(id) ON DELETE SET NULL,
    iam_identity_id     BIGINT       REFERENCES iam_identities(id) ON DELETE SET NULL,
    response_type       VARCHAR(100) NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    execution_details   JSONB,
    executed_by         VARCHAR(100),
    executed_at         TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_history (
    id              BIGSERIAL    PRIMARY KEY,
    report_type     VARCHAR(100) NOT NULL,
    generated_by    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    parameters      JSONB,
    record_count    INT          DEFAULT 0,
    file_path       VARCHAR(500),
    generated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_settings (
    id              BIGSERIAL    PRIMARY KEY,
    setting_key     VARCHAR(200) NOT NULL UNIQUE,
    setting_value   TEXT,
    value_type      VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    description     TEXT,
    updated_by      BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anomaly_events_identity  ON anomaly_events(iam_identity_id, created_at DESC);
CREATE INDEX idx_anomaly_events_severity  ON anomaly_events(severity, created_at DESC);
CREATE INDEX idx_anomaly_events_ack       ON anomaly_events(is_acknowledged, created_at DESC);
CREATE INDEX idx_alert_notif_user         ON alert_notifications(user_id, is_read);
CREATE INDEX idx_alert_notif_created      ON alert_notifications(created_at DESC);
CREATE INDEX idx_auto_responses_identity  ON automated_responses(iam_identity_id, created_at DESC);
CREATE INDEX idx_auto_responses_status    ON automated_responses(status);
CREATE INDEX idx_report_history_type      ON report_history(report_type, generated_at DESC);
CREATE INDEX idx_system_settings_key      ON system_settings(setting_key);
