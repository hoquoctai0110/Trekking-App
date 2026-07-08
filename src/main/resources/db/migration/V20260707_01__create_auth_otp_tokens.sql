CREATE TABLE IF NOT EXISTS auth_otp_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    resend_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_otp_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX IF NOT EXISTS idx_auth_otp_tokens_user_purpose_created
    ON auth_otp_tokens (user_id, purpose, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_auth_otp_tokens_expires_at
    ON auth_otp_tokens (expires_at);
