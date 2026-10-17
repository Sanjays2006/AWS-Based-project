-- ============================================================
-- V13: CloudTrail event de-duplication support
-- Adds a unique event identifier to activity_logs so repeated
-- ingestion runs (CloudTrail LookupEvents API and S3 log files)
-- never persist the same CloudTrail event twice.
-- ============================================================

ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS event_id VARCHAR(100);

-- Partial unique index: enforces uniqueness only for non-null event ids,
-- so legacy rows without an event id are unaffected.
CREATE UNIQUE INDEX IF NOT EXISTS ux_activity_logs_event_id
    ON activity_logs (event_id)
    WHERE event_id IS NOT NULL;

-- Supports time-window queries used by ingestion and dashboards.
CREATE INDEX IF NOT EXISTS idx_activity_logs_event_time
    ON activity_logs (event_time);
