-- ============================================================
-- V6: Seed Data — Roles, Risk Rules, System Settings
-- ============================================================

-- ===== Roles =====
INSERT INTO roles (name, description) VALUES
('ADMIN',    'System administrator with full access to all features and configurations'),
('ANALYST',  'Security analyst with read, acknowledge, and investigation access'),
('VIEWER',   'Read-only access to dashboards, reports, and activity logs')
ON CONFLICT (name) DO NOTHING;

-- ===== Risk Rules (13 weighted rules) =====
INSERT INTO risk_rules (rule_code, description, weight, category, is_active) VALUES
('NEW_COUNTRY',             'Login from a country not previously seen for this identity', 30, 'LOCATION',  TRUE),
('NEW_DEVICE',              'Login from an unrecognized device fingerprint',               20, 'DEVICE',    TRUE),
('MIDNIGHT_LOGIN',          'Login activity between 00:00 and 05:00 local time',           15, 'TIME',      TRUE),
('WEEKEND_LOGIN',           'Login activity on Saturday or Sunday',                        10, 'TIME',      TRUE),
('PRIVILEGE_ESCALATION',    'IAM privilege escalation API call detected',                  40, 'PRIVILEGE', TRUE),
('MASS_DOWNLOAD',           'Data download volume exceeds 2x the average baseline',        35, 'BEHAVIOR',  TRUE),
('MULTIPLE_FAILED_LOGINS',  'Five or more failed login attempts within a 30-minute window',25, 'BEHAVIOR',  TRUE),
('NEW_REGION',              'API call from an AWS region not previously used',             15, 'LOCATION',  TRUE),
('UNUSUAL_SERVICE',         'Access to an AWS service not in the typical service set',     20, 'BEHAVIOR',  TRUE),
('IMPOSSIBLE_TRAVEL',       'Login from two different countries within one hour',          50, 'LOCATION',  TRUE),
('LONG_SESSION',            'Active session exceeds 3x the average session duration',      10, 'BEHAVIOR',  TRUE),
('UNUSUAL_API_SEQUENCE',    'Detected an unusual sequence of API calls vs. baseline',      25, 'BEHAVIOR',  TRUE),
('VPN_OR_TOR',              'Source IP address identified as a VPN or Tor exit node',      20, 'LOCATION',  TRUE)
ON CONFLICT (rule_code) DO NOTHING;

-- ===== System Settings =====
INSERT INTO system_settings (setting_key, setting_value, value_type, description) VALUES
('risk.critical.threshold',          '76',    'INT',     'Minimum risk score for CRITICAL level'),
('risk.high.threshold',              '51',    'INT',     'Minimum risk score for HIGH level'),
('risk.medium.threshold',            '26',    'INT',     'Minimum risk score for MEDIUM level'),
('anomaly.baseline.days',            '30',    'INT',     'Number of days used to compute behavior baseline'),
('anomaly.detection.enabled',        'true',  'BOOLEAN', 'Global toggle for anomaly detection'),
('notification.auto.send',           'true',  'BOOLEAN', 'Automatically send in-app notifications on anomaly'),
('session.max.duration.hours',       '12',    'INT',     'Maximum allowed session duration before forced logout'),
('cloudtrail.poll.interval.minutes', '5',     'INT',     'CloudTrail S3 polling interval in minutes'),
('fingerprint.recompute.hours',      '1',     'INT',     'Frequency of fingerprint recomputation in hours'),
('report.daily.enabled',             'true',  'BOOLEAN', 'Enable daily automated reports')
ON CONFLICT (setting_key) DO NOTHING;
