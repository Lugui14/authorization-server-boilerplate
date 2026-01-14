-- Insert default admin user
-- Password: admin123 (BCrypt encoded)
INSERT INTO users (email, password, name, enabled, account_non_expired, account_non_locked, credentials_non_expired, provider)
VALUES ('admin@example.com', '$2a$10$XzqZjJHGq7fS7gKVVT5qb.Yl5WvP8bKVqzMPYz5iZYKCQJmqS1sYa', 'Admin User', true, true, true, true, 'LOCAL');

-- Insert default regular user
-- Password: user123 (BCrypt encoded)
INSERT INTO users (email, password, name, enabled, account_non_expired, account_non_locked, credentials_non_expired, provider)
VALUES ('user@example.com', '$2a$10$rGhCqKvLzKqVqVGQ8YGMxe5LYkJ8XqVZqzMPYz5iZYKCQJmqS1sYa', 'Regular User', true, true, true, true, 'LOCAL');

-- Assign roles
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (1, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (2, 'USER');

