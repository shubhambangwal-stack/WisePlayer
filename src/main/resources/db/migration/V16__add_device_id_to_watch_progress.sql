-- V16__add_device_id_to_watch_progress.sql
-- The watch_progress table on the live server was created before V15 added
-- device_id, so Flyway considers V15 as "already applied" but the column is
-- missing. This migration safely adds all missing pieces.

-- 1. Add device_id column (nullable first so existing rows don't violate NOT NULL)
ALTER TABLE watch_progress
    ADD COLUMN IF NOT EXISTS device_id UUID;

-- 2. For any orphaned rows (no device), fall back to a sentinel / delete them.
--    Since watch_progress data is non-critical (playback positions only),
--    we simply delete rows that have no device_id so we can enforce NOT NULL.
DELETE FROM watch_progress WHERE device_id IS NULL;

-- 3. Now enforce NOT NULL
ALTER TABLE watch_progress
    ALTER COLUMN device_id SET NOT NULL;

-- 4. Add the unique constraint (device, stream, type) if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_watch_progress_device_stream'
    ) THEN
        ALTER TABLE watch_progress
            ADD CONSTRAINT uq_watch_progress_device_stream
            UNIQUE (device_id, stream_id, stream_type);
    END IF;
END $$;

-- 5. Add the foreign-key constraint to devices if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_watch_progress_device'
    ) THEN
        ALTER TABLE watch_progress
            ADD CONSTRAINT fk_watch_progress_device
            FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE;
    END IF;
END $$;

-- 6. Add index for fast look-ups by device_id
CREATE INDEX IF NOT EXISTS idx_watch_progress_device_id ON watch_progress (device_id);
