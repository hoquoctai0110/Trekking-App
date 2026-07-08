ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS schedule_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_bookings_schedule'
          AND table_name = 'bookings'
    ) THEN
        ALTER TABLE bookings
            ADD CONSTRAINT fk_bookings_schedule
            FOREIGN KEY (schedule_id) REFERENCES tour_schedules (schedule_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bookings_schedule_id
    ON bookings (schedule_id);
