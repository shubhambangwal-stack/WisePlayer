package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single EPG programme for a channel.
 *
 * <p>{@code catchupAvailable} is only {@code true} for past / currently running
 * programmes on a channel whose provider actually exposes archive data. The
 * progress fields are only populated for the currently running programme.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpgProgram {

    private String id;
    private String channelId;
    private String title;
    private String description;
    private long startTs;
    private long endTs;
    private boolean isRunning;
    private boolean isPast;
    private boolean catchupAvailable;
    private Integer progressPercent;
    private Long remainingSeconds;

    public EpgProgram() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartTs() {
        return startTs;
    }

    public void setStartTs(long startTs) {
        this.startTs = startTs;
    }

    public long getEndTs() {
        return endTs;
    }

    public void setEndTs(long endTs) {
        this.endTs = endTs;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isPast() {
        return isPast;
    }

    public void setPast(boolean past) {
        isPast = past;
    }

    public boolean isCatchupAvailable() {
        return catchupAvailable;
    }

    public void setCatchupAvailable(boolean catchupAvailable) {
        this.catchupAvailable = catchupAvailable;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}