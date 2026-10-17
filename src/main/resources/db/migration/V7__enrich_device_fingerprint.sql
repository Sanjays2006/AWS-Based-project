-- V7: Enrich device fingerprinting with screen resolution and timezone
ALTER TABLE devices
ADD COLUMN IF NOT EXISTS screen_resolution VARCHAR(30) DEFAULT '1920x1080',
ADD COLUMN IF NOT EXISTS timezone VARCHAR(100) DEFAULT 'UTC';
