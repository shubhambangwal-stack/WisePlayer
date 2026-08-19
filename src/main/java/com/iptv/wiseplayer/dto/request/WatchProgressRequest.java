package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Payload sent by the client to record the current playback position.
 *
 * <p>The client should call {@code POST /api/progress} periodically while
 * playing (e.g. every 10–15 seconds) and once more on pause / exit.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code streamId}       – Xtream numeric ID (VOD stream_id or Series episode id)</li>
 *   <li>{@code streamType}     – {@code "VOD"} or {@code "SERIES"}</li>
 *   <li>{@code positionSeconds} – How far the user has watched (in seconds)</li>
 *   <li>{@code durationSeconds} – Total length of the content (in seconds)</li>
 * </ul>
 */
public class WatchProgressRequest {

    @NotNull(message = "playlistId is required")
    private UUID playlistId;

    @NotNull(message = "streamId is required")
    private Integer streamId;

    @NotBlank(message = "streamType is required")
    @Pattern(regexp = "VOD|SERIES", message = "streamType must be 'VOD' or 'SERIES'")
    private String streamType;

    @Min(value = 0, message = "positionSeconds must be ≥ 0")
    private long positionSeconds;

    @Min(value = 1, message = "durationSeconds must be > 0")
    private long durationSeconds;

    public WatchProgressRequest() {}

    public UUID getPlaylistId() { return playlistId; }
    public void setPlaylistId(UUID playlistId) { this.playlistId = playlistId; }

    public Integer getStreamId() { return streamId; }
    public void setStreamId(Integer streamId) { this.streamId = streamId; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public long getPositionSeconds() { return positionSeconds; }
    public void setPositionSeconds(long positionSeconds) { this.positionSeconds = positionSeconds; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
}
