-- ============================================================
-- V4: Behavior Fingerprints, Behavior History, Risk Rules, Risk Scores
-- ============================================================

-- 10-Dimension Behavior Fingerprint
CREATE TABLE behavior_fingerprints (
    id                                  BIGSERIAL       PRIMARY KEY,
    iam_identity_id                     BIGINT          NOT NULL UNIQUE REFERENCES iam_identities(id) ON DELETE CASCADE,

    -- Dimension 1: Time
    typical_hours_start                 INT,
    typical_hours_end                   INT,
    typical_weekdays                    VARCHAR(50),

    -- Dimension 2: Location
    typical_regions                     JSONB,
    typical_countries                   JSONB,
    typical_ip_ranges                   JSONB,

    -- Dimension 3: Device
    typical_device_fingerprints         JSONB,
    typical_browsers                    JSONB,
    typical_os_types                    JSONB,

    -- Dimension 4: AWS Service Usage
    typical_services                    JSONB,

    -- Dimension 5: API Calls
    typical_api_calls                   JSONB,

    -- Dimension 6: Action Sequences
    typical_resource_sequences          JSONB,

    -- Dimension 7: Volume
    avg_daily_api_calls                 DECIMAL(10,2)   DEFAULT 0,
    avg_data_download_bytes             BIGINT          DEFAULT 0,
    max_data_download_bytes             BIGINT          DEFAULT 0,

    -- Dimension 8: Session
    avg_session_duration_minutes        DECIMAL(10,2)   DEFAULT 0,

    -- Dimension 9: Login Frequency
    avg_logins_per_day                  DECIMAL(5,2)    DEFAULT 0,
    avg_failed_logins_per_day           DECIMAL(5,2)    DEFAULT 0,

    -- Dimension 10: Privilege
    avg_privilege_escalations_per_week  DECIMAL(5,2)    DEFAULT 0,
    known_high_privilege_actions        JSONB,

    -- Metadata
    baseline_computed_at                TIMESTAMP,
    updated_at                          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Daily behavior snapshots (historical tracking)
CREATE TABLE behavior_history (
    id                      BIGSERIAL       PRIMARY KEY,
    iam_identity_id         BIGINT          NOT NULL REFERENCES iam_identities(id) ON DELETE CASCADE,
    snapshot_date           DATE            NOT NULL,
    api_call_count          INT             NOT NULL DEFAULT 0,
    login_count             INT             NOT NULL DEFAULT 0,
    failed_login_count      INT             NOT NULL DEFAULT 0,
    unique_regions          INT             NOT NULL DEFAULT 0,
    unique_devices          INT             NOT NULL DEFAULT 0,
    privilege_escalations   INT             NOT NULL DEFAULT 0,
    data_download_bytes     BIGINT          NOT NULL DEFAULT 0,
    session_count           INT             NOT NULL DEFAULT 0,
    avg_session_minutes     DECIMAL(10,2)   DEFAULT 0,
    risk_score_eod          DECIMAL(5,2),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (iam_identity_id, snapshot_date)
);

-- Configurable risk rules with weights
CREATE TABLE risk_rules (
    id              BIGSERIAL    PRIMARY KEY,
    rule_code       VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    weight          INT          NOT NULL DEFAULT 10,
    category        VARCHAR(50)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Risk scores (time-series)
CREATE TABLE risk_scores (
    id                  BIGSERIAL       PRIMARY KEY,
    iam_identity_id     BIGINT          NOT NULL REFERENCES iam_identities(id) ON DELETE CASCADE,
    score               DECIMAL(5,2)    NOT NULL,
    risk_level          VARCHAR(20)     NOT NULL,
    triggered_rules     JSONB,
    score_factors       JSONB,
    computed_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_behavior_history_identity  ON behavior_history(iam_identity_id, snapshot_date DESC);
CREATE INDEX idx_risk_rules_category        ON risk_rules(category, is_active);
CREATE INDEX idx_risk_scores_identity       ON risk_scores(iam_identity_id, computed_at DESC);
CREATE INDEX idx_risk_scores_level          ON risk_scores(risk_level, computed_at DESC);
CREATE INDEX idx_risk_scores_computed       ON risk_scores(computed_at DESC);
