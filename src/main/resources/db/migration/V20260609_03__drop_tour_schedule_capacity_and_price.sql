ALTER TABLE IF EXISTS tour_schedules
    DROP COLUMN IF EXISTS max_participants;

ALTER TABLE IF EXISTS tour_schedules
    DROP COLUMN IF EXISTS price;
