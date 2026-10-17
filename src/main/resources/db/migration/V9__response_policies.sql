-- V9: Configurable Automated Response Policies
CREATE TABLE IF NOT EXISTS response_policies (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(100) NOT NULL,
    risk_level_trigger VARCHAR(50) NOT NULL,
    min_risk_score DECIMAL(5, 2) NOT NULL,
    response_action VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default policies matching viva recommendation model
INSERT INTO response_policies (policy_name, risk_level_trigger, min_risk_score, response_action, description, priority) VALUES
('Critical Threat Quarantine', 'CRITICAL', 76.00, 'QUARANTINE', 'Automatically attach Deny-All policy and revoke tokens when risk > 75', 1),
('High Risk Session Revocation', 'HIGH', 51.00, 'REVOKE_SESSION', 'Revoke active console sessions and require re-authentication when risk 51-75', 2),
('Medium Risk MFA Challenge', 'MEDIUM', 26.00, 'REQUIRE_MFA', 'Force step-up TOTP MFA verification when risk 26-50', 3),
('Low Risk Security Alert', 'LOW', 0.00, 'NOTIFY_ONLY', 'Generate in-app notification without revoking access for risk < 26', 4)
ON CONFLICT DO NOTHING;
