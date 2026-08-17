package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores the last-watched position for a single VOD or Series episode
 * per playlist. One row per (playlist_id, stream_id, stream_type) — never grows
 * into a history log. The service layer upserts this row on every progress save.
 *
 * <p>When the user watches ≥ 95 % of the content, {@code fullyWatched} is set to
 * {@code true} and {@code positionSeconds} is reset to 0 so the next play starts
 * from the beginning.</p>
 */
@Entity
@Table(
    name = "watch_progress",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_watch_progress_playlist_stream",
            columnNames = {"playlist_id", "stream_id", "stream_type"}
        )
    },
    indexes = {
        @Index(name = "idx_watch_progress_playlist_id", columnList = "playlist_id")
    }
)
public class WatchProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The playlist (= user/device context) that owns this progress entry. */
    @Column(name = "playlist_id", nullable = false)
    private UUID playlistId;

    /**
     * Xtream Codes stream ID.
     * For VOD this is the {@code stream_id} field; for Series episodes this is the
     * numeric {@code id} from the {@code get_series_info} episode response.
     */
    @Column(name = "stream_id", nullable = false)
    private int streamId;

    /** VOD or SERIES – never LIVE. */
    @Column(name = "stream_type", nullable = false, length = 10)
    private String streamType;

    /**
     * The second at which the user stopped watching.
     * Resets to 0 when the content is marked as fully watched.
     */
    @Column(name = "position_seconds", nullable = false)
    private long positionSeconds;

    /**
     * Total duration in seconds as reported by the client.
     * Used to compute the watched percentage on the server side.
     */
    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    /**
     * {@code true} when the user has watched ≥ 95 % of the content.
     * When this flag is {@code true}, {@code positionSeconds} is 0, meaning the
     * next play will start from the beginning automatically.
     */
    @Column(name = "fully_watched", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fullyWatched = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WatchProgress() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPlaylistId() { return playlistId; }
    public void setPlaylistId(UUID playlistId) { this.playlistId = playlistId; }

    public int getStreamId() { return streamId; }
    public void setStreamId(int streamId) { this.streamId = streamId; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public long getPositionSeconds() { return positionSeconds; }
    public void setPositionSeconds(long positionSeconds) { this.positionSeconds = positionSeconds; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isFullyWatched() { return fullyWatched; }
    public void setFullyWatched(boolean fullyWatched) { this.fullyWatched = fullyWatched; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
