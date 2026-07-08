UPDATE bookings
SET booking_status = 'PENDING_PAYMENT'
WHERE booking_status = 'PENDING';

UPDATE bookings
SET payment_status = 'PENDING'
WHERE payment_status = 'UNPAID';

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    order_code BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payos_payment_link_id VARCHAR(100),
    transaction_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payments_booking UNIQUE (booking_id),
    CONSTRAINT uk_payments_order_code UNIQUE (order_code),
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_order_code
    ON payments (order_code);

CREATE INDEX IF NOT EXISTS idx_payments_status
    ON payments (status);
