CREATE TABLE IF NOT EXISTS tracking_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tour_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    total_distance_km DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_latitude DOUBLE PRECISION,
    last_longitude DOUBLE PRECISION,
    last_location_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tracking_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_tracking_sessions_tour
        FOREIGN KEY (tour_id) REFERENCES tours (tour_id),
    CONSTRAINT fk_tracking_sessions_route
        FOREIGN KEY (route_id) REFERENCES routes (route_id),
    CONSTRAINT fk_tracking_sessions_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);

CREATE TABLE IF NOT EXISTS tracking_points (
    point_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tracking_points_session
        FOREIGN KEY (session_id) REFERENCES tracking_sessions (session_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tracking_sessions_user_id
    ON tracking_sessions (user_id);

CREATE INDEX IF NOT EXISTS idx_tracking_sessions_booking_status
    ON tracking_sessions (booking_id, status);

CREATE INDEX IF NOT EXISTS idx_tracking_points_session_recorded_at
    ON tracking_points (session_id, recorded_at DESC, point_id DESC);
