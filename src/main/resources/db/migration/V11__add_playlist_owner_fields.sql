-- V11__add_playlist_owner_fields.sql
-- Adds ownership tracking columns to the playlists table so that
-- Admins, Resellers, and Sub-Resellers can own playlists before
-- assigning them to a device.

-- 1. Drop the NOT NULL constraint on device_id so playlists can
--    exist independently before being assigned to a device.
ALTER TABLE playlists
    ALTER COLUMN device_id DROP NOT NULL;

-- 2. Add owner_type to identify who created/owns the playlist.
--    Values: ADMIN, RESELLER, SUB_RESELLER, SYSTEM, DEVICE
ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS owner_type VARCHAR(30);

-- 3. Add owner_id (UUID) referencing the Admin/Reseller who owns it.
ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS owner_id UUID;

-- 4. Add index for efficient owner-based queries.
CREATE INDEX IF NOT EXISTS idx_playlist_owner ON playlists (owner_id, owner_type);
