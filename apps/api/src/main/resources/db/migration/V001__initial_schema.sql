-- Initial database schema

-- Users table
CREATE TABLE users (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    avatar_url          VARCHAR(500),
    role                VARCHAR(20) NOT NULL DEFAULT 'USER',
    account_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING_EMAIL',
    email_verified_at   TIMESTAMP WITH TIME ZONE,
    approved_at         TIMESTAMP WITH TIME ZONE,
    approved_by         BIGINT REFERENCES users(id),
    verification_token  VARCHAR(255),
    token_expires_at    TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;