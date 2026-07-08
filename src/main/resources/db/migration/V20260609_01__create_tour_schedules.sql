CREATE TABLE IF NOT EXISTS tour_schedules (
    schedule_id BIGSERIAL PRIMARY KEY,
    tour_id BIGINT NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    booked_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tour_schedules_tour
        FOREIGN KEY (tour_id) REFERENCES tours (tour_id)
);

CREATE INDEX IF NOT EXISTS idx_tour_schedules_tour_id
    ON tour_schedules (tour_id);

CREATE INDEX IF NOT EXISTS idx_tour_schedules_status_start
    ON tour_schedules (status, start_date_time);
