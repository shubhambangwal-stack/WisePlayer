package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;

/**
 * Per-channel catch-up / archive availability.
 *
 * <p>Unlike {@link CatchUpStatus}, this describes a single channel and is used
 * to show the "Catch-Up" badge on the channel itself. {@code playable} is only
 * {@code true} when a catch-up stream URL can actually be constructed from the
 * data the provider supplied.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatchUpChannelStatus {

    private String channelId;
    private boolean supported;
    private boolean playable;
    private CatchUpMethod method;
    private Integer days;
    private String source;
    private String epgChannelId;
    private String liveUrl;

    public CatchUpChannelStatus() {
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public boolean isPlayable() {
        return playable;
    }

    public void setPlayable(boolean playable) {
        this.playable = playable;
    }

    public CatchUpMethod getMethod() {
        return method;
    }

    public void setMethod(CatchUpMethod method) {
        this.method = method;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEpgChannelId() {
        return epgChannelId;
    }

    public void setEpgChannelId(String epgChannelId) {
        this.epgChannelId = epgChannelId;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }
}