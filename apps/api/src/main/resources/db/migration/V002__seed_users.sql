-- Seed users for development
-- Passwords are BCrypt hashed

INSERT INTO users (email, password_hash, first_name, last_name, role, account_status, email_verified_at, approved_at)
VALUES
    -- Normal user: user@example.com / user
    ('user@example.com',
     '$2b$10$Vj32ajZYUhfqbzuMRMW2YORNF1TCLZf89eqYD7wgF2zHPy8VzO3Ta',
     'Demo', 'User', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Admin user: admin@example.com / admin
    ('admin@example.com',
     '$2b$10$03HYI5VAzry6ixvBjC.1GO2MNkhIF1gDH0c9QUoLeKCDlJH16DtLy',
     'Admin', 'User', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);