-- Refresh token rotation with per-device tracking.
-- A refresh token is single-use: /auth/refresh revokes the presented token and issues a new one.
-- Reuse of an already-revoked token is treated as theft and revokes the whole user's family.

CREATE TABLE refresh_tokens (
    id                 BIGSERIAL    PRIMARY KEY,
    token              VARCHAR(36)  NOT NULL UNIQUE,
    user_id            BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at         TIMESTAMPTZ  NOT NULL,
    revoked            BOOLEAN      NOT NULL DEFAULT FALSE,
    remember_me        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    device_fingerprint VARCHAR(64),
    device_label       VARCHAR(255),
    device_summary     VARCHAR(120)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Supports revoking the previous session for the same device on a fresh login.
CREATE INDEX idx_refresh_tokens_user_fingerprint
    ON refresh_tokens (user_id, device_fingerprint)
    WHERE device_fingerprint IS NOT NULL;

-- Supports the scheduled purge of stale rows.
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
