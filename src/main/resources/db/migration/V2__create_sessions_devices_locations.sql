-- ============================================================
-- V2: User Sessions, Devices, Login Locations
-- ============================================================

CREATE TABLE devices (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint  VARCHAR(500) NOT NULL,
    browser             VARCHAR(100),
    operating_system    VARCHAR(100),
    device_type         VARCHAR(50),
    is_trusted          BOOLEAN      NOT NULL DEFAULT FALSE,
    first_seen_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at        TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_sessions (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token       VARCHAR(500) NOT NULL UNIQUE,
    device_id           BIGINT       REFERENCES devices(id) ON DELETE SET NULL,
    ip_address          VARCHAR(50),
    login_at            TIMESTAMP    NOT NULL,
    logout_at           TIMESTAMP,
    last_activity_at    TIMESTAMP,
    duration_minutes    INT,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE login_locations (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id      BIGINT          REFERENCES user_sessions(id) ON DELETE SET NULL,
    ip_address      VARCHAR(50)     NOT NULL,
    country_code    VARCHAR(10),
    country_name    VARCHAR(100),
    city            VARCHAR(100),
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    is_vpn          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_tor          BOOLEAN         NOT NULL DEFAULT FALSE,
    login_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_devices_user_id           ON devices(user_id);
CREATE INDEX idx_devices_fingerprint       ON devices(device_fingerprint);
CREATE INDEX idx_user_sessions_user        ON user_sessions(user_id, is_active);
CREATE INDEX idx_user_sessions_login_at    ON user_sessions(login_at DESC);
CREATE INDEX idx_user_sessions_token       ON user_sessions(session_token);
CREATE INDEX idx_login_locations_user      ON login_locations(user_id, login_at DESC);
CREATE INDEX idx_login_locations_country   ON login_locations(country_code);
