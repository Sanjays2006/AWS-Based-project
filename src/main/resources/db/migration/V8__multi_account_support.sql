-- V8: Multi-Account AWS Support
CREATE TABLE IF NOT EXISTS aws_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_alias VARCHAR(100),
    region VARCHAR(50) NOT NULL DEFAULT 'us-east-1',
    access_key_id VARCHAR(100),
    secret_access_key VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE iam_identities
ADD COLUMN IF NOT EXISTS aws_account_id BIGINT REFERENCES aws_accounts(id) ON DELETE SET NULL;

ALTER TABLE activity_logs
ADD COLUMN IF NOT EXISTS account_id VARCHAR(20);

-- Seed primary demo AWS account
INSERT INTO aws_accounts (account_id, account_name, account_alias, region, is_active)
VALUES ('123456789012', 'Production AWS Account', 'prod-aws', 'us-east-1', true)
ON CONFLICT (account_id) DO NOTHING;
