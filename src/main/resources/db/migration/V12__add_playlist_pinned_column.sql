-- V12__add_playlist_pinned_column.sql
-- Adds the 'pinned' boolean column to the playlists table.
-- This column tracks which playlist is pinned per device (one pin per device).

ALTER TABLE playlists
    ADD COLUMN IF NOT EXISTS pinned BOOLEAN NOT NULL DEFAULT FALSE;
