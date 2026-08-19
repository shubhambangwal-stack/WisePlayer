package com.iptv.wiseplayer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO attached to every VOD stream and Series episode response
 * when the user has a saved watch position.
 *
 * <p>The frontend uses these values to:</p>
 * <ul>
 *   <li>Render a progress bar (using {@code watchedPercent}).</li>
 *   <li>Automatically seek to {@code resumePositionSeconds} on play.</li>
 * </ul>
 *
 * <p>When the content is fully watched ({@code fullyWatched = true}),
 * {@code resumePositionSeconds} is 0 — the next play starts from the
 * beginning, exactly like YouTube's behaviour.</p>
 */
public class WatchProgressResponse {

    /** The exact second to seek to on playback start. 0 if not started or fully watched. */
    @JsonProperty("resume_position_seconds")
    private long resumePositionSeconds;

    /** Total duration in seconds. */
    @JsonProperty("duration_seconds")
    private long durationSeconds;

    /** 0.0 – 100.0 percentage of content watched. */
    @JsonProperty("watched_percent")
    private double watchedPercent;

    /** True when the user has watched ≥ 95 % of the content. */
    @JsonProperty("fully_watched")
    private boolean fullyWatched;

    public WatchProgressResponse() {}

    public WatchProgressResponse(long resumePositionSeconds, long durationSeconds,
                                 double watchedPercent, boolean fullyWatched) {
        this.resumePositionSeconds = resumePositionSeconds;
        this.durationSeconds = durationSeconds;
        this.watchedPercent = watchedPercent;
        this.fullyWatched = fullyWatched;
    }

    public long getResumePositionSeconds() { return resumePositionSeconds; }
    public void setResumePositionSeconds(long resumePositionSeconds) {
        this.resumePositionSeconds = resumePositionSeconds;
    }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getWatchedPercent() { return watchedPercent; }
    public void setWatchedPercent(double watchedPercent) { this.watchedPercent = watchedPercent; }

    public boolean isFullyWatched() { return fullyWatched; }
    public void setFullyWatched(boolean fullyWatched) { this.fullyWatched = fullyWatched; }
}
