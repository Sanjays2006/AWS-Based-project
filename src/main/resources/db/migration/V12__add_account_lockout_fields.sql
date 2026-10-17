-- ============================================================
-- V12: Account Lockout & Brute-Force Security Fields
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS lockout_time TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_account_locked ON users(account_non_locked);
