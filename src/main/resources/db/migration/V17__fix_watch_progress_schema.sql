-- V17__fix_watch_progress_schema.sql
-- Re-adds playlist_id if missing and fixes the composite unique constraint
-- so that progress is keyed correctly by (device_id, playlist_id, stream_id, stream_type).
-- This ensures that the same stream_id on two different playlists for the same device
-- do not overwrite each other.

-- 1. Ensure playlist_id exists (it might already exist on live servers, but missing locally)
ALTER TABLE watch_progress
    ADD COLUMN IF NOT EXISTS playlist_id UUID;

-- 2. Drop any orphaned rows if playlist_id is null, so we can enforce NOT NULL safely
DELETE FROM watch_progress WHERE playlist_id IS NULL;

-- 3. Enforce NOT NULL on playlist_id
ALTER TABLE watch_progress
    ALTER COLUMN playlist_id SET NOT NULL;

-- 4. Drop the old unique constraint (from V15/V16) which lacked playlist_id
ALTER TABLE watch_progress
    DROP CONSTRAINT IF EXISTS uq_watch_progress_device_stream;

-- 5. Create the NEW unique constraint incorporating playlist_id
ALTER TABLE watch_progress
    ADD CONSTRAINT uq_watch_progress_device_playlist_stream
    UNIQUE (device_id, playlist_id, stream_id, stream_type);
