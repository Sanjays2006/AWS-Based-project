-- ============================================================
-- V1: Users, Roles, User-Role join table
-- ============================================================

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id              BIGSERIAL    PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    mfa_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret      VARCHAR(100),
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_users_username  ON users(username);
CREATE INDEX idx_users_email     ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
