-- V15__create_watch_progress_table.sql

CREATE TABLE watch_progress (
    id UUID PRIMARY KEY,
    playlist_id UUID NOT NULL,
    stream_id INTEGER NOT NULL,
    stream_type VARCHAR(10) NOT NULL,
    position_seconds BIGINT NOT NULL,
    duration_seconds BIGINT NOT NULL,
    fully_watched BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_watch_progress_playlist_stream UNIQUE (playlist_id, stream_id, stream_type),
    CONSTRAINT fk_watch_progress_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(playlist_id) ON DELETE CASCADE
);

CREATE INDEX idx_watch_progress_playlist_id ON watch_progress (playlist_id);
