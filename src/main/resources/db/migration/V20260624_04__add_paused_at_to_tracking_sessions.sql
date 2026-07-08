ALTER TABLE tracking_sessions
    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP;
