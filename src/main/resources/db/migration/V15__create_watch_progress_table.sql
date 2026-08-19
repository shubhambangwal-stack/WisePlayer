-- V15__create_watch_progress_table.sql
-- Stores the last-watched position per physical device (device_id) per stream.
-- Keyed by device_id (not playlist_id) so devices sharing the same playlist
-- each get their own independent watch position row.

CREATE TABLE watch_progress (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    stream_id INTEGER NOT NULL,
    stream_type VARCHAR(10) NOT NULL,
    position_seconds BIGINT NOT NULL,
    duration_seconds BIGINT NOT NULL,
    fully_watched BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_watch_progress_device_stream UNIQUE (device_id, stream_id, stream_type),
    CONSTRAINT fk_watch_progress_device FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

CREATE INDEX idx_watch_progress_device_id ON watch_progress (device_id);
