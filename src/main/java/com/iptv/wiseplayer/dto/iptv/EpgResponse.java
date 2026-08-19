package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * EPG response for a single channel, including past programmes when the
 * provider exposes archive data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpgResponse {

    private String channelId;
    private boolean catchupSupported;
    private boolean catchupPlayable;
    private CatchUpMethod catchupMethod;
    private Integer catchupDays;
    private long serverTime;
    private long liveEdge;
    private String liveUrl;
    private List<EpgProgram> programs = new ArrayList<>();

    public EpgResponse() {
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isCatchupSupported() {
        return catchupSupported;
    }

    public void setCatchupSupported(boolean catchupSupported) {
        this.catchupSupported = catchupSupported;
    }

    public boolean isCatchupPlayable() {
        return catchupPlayable;
    }

    public void setCatchupPlayable(boolean catchupPlayable) {
        this.catchupPlayable = catchupPlayable;
    }

    public CatchUpMethod getCatchupMethod() {
        return catchupMethod;
    }

    public void setCatchupMethod(CatchUpMethod catchupMethod) {
        this.catchupMethod = catchupMethod;
    }

    public Integer getCatchupDays() {
        return catchupDays;
    }

    public void setCatchupDays(Integer catchupDays) {
        this.catchupDays = catchupDays;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public long getLiveEdge() {
        return liveEdge;
    }

    public void setLiveEdge(long liveEdge) {
        this.liveEdge = liveEdge;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }

    public List<EpgProgram> getPrograms() {
        return programs;
    }

    public void setPrograms(List<EpgProgram> programs) {
        this.programs = programs;
    }
}