-- Create user_roles junction table for multiple roles per user
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Migrate existing roles from users table
INSERT INTO user_roles (user_id, role)
SELECT id, role FROM users;

-- Drop the role column from users table
ALTER TABLE users DROP COLUMN role;