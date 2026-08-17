-- V14__add_catchup_fields_to_playlists.sql
-- Adds cached catch-up / archive capability metadata to the playlists table.
-- This snapshot is refreshed periodically by CatchUpService to reduce API calls
-- to the upstream IPTV provider.

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS catchup_supported BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS catchup_method VARCHAR(20);

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS catchup_days INT;

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS catchup_source TEXT;

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS catchup_checked_at TIMESTAMP;