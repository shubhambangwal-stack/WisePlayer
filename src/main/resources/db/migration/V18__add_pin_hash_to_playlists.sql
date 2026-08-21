-- Add pin_hash column to playlists table for playlist-level PIN functionality
ALTER TABLE playlists
ADD COLUMN pin_hash VARCHAR(100);
