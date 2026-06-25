-- ============================================================
-- Migration: Create role_permissions table
-- This table stores the DEFAULT CRUD flags per AdminRole.
-- When a new admin/reseller/sub-reseller is created, their
-- initial permission flags are read from here instead of
-- being hardcoded to TRUE in the Java entity.
-- ============================================================

CREATE TABLE IF NOT EXISTS role_permissions (
    role        VARCHAR(20)  PRIMARY KEY,
    can_create  BOOLEAN      NOT NULL DEFAULT TRUE,
    can_read    BOOLEAN      NOT NULL DEFAULT TRUE,
    can_update  BOOLEAN      NOT NULL DEFAULT TRUE,
    can_delete  BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Seed one row per role (all true = same as current hardcoded behaviour)
INSERT INTO role_permissions (role, can_create, can_read, can_update, can_delete)
VALUES
    ('SUPER_ADMIN',  TRUE, TRUE, TRUE, TRUE),
    ('ADMIN',        TRUE, TRUE, TRUE, TRUE),
    ('RESELLER',     TRUE, TRUE, TRUE, TRUE),
    ('SUB_RESELLER', TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (role) DO NOTHING;
