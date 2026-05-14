-- V3__add_superadmin_email_and_reset_tokens.sql

-- Add email to super_admins
ALTER TABLE super_admins ADD COLUMN email VARCHAR(100) UNIQUE;

-- Password Reset Tokens Table
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_email ON password_reset_tokens (email);
