CREATE TABLE IF NOT EXISTS sos_alerts (
    sos_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    tracking_session_id BIGINT,
    tour_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    client_request_id VARCHAR(100),
    client_created_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_sos_alerts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_sos_alerts_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id),
    CONSTRAINT fk_sos_alerts_tracking_session
        FOREIGN KEY (tracking_session_id) REFERENCES tracking_sessions (session_id),
    CONSTRAINT fk_sos_alerts_tour
        FOREIGN KEY (tour_id) REFERENCES tours (tour_id),
    CONSTRAINT uk_sos_alerts_user_client_request
        UNIQUE (user_id, client_request_id)
);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_user_id
    ON sos_alerts (user_id);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_booking_id
    ON sos_alerts (booking_id);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_tour_id
    ON sos_alerts (tour_id);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_status
    ON sos_alerts (status);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_created_at
    ON sos_alerts (created_at);
