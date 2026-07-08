ALTER TABLE tracking_sessions
    ADD COLUMN IF NOT EXISTS direction VARCHAR(20) NOT NULL DEFAULT 'OUTBOUND';

CREATE INDEX IF NOT EXISTS idx_tracking_sessions_booking_direction_status
    ON tracking_sessions (booking_id, direction, status);
