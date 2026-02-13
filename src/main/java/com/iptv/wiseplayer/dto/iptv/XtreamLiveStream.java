package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class XtreamLiveStream {
    @JsonProperty("num")
    private int num;
    @JsonProperty("name")
    private String name;
    @JsonProperty("stream_type")
    private String streamType;
    @JsonProperty("stream_id")
    private int streamId;
    @JsonProperty("stream_icon")
    private String streamIcon;
    @JsonProperty("epg_channel_id")
    private String epgChannelId;
    @JsonProperty("added")
    private String added;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("custom_sid")
    private String customSid;
    @JsonProperty("tv_archive")
    private int tvArchive;
    @JsonProperty("direct_source")
    private String directSource;
    @JsonProperty("tv_archive_duration")
    private int tvArchiveDuration;

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public String getStreamIcon() {
        return streamIcon;
    }

    public void setStreamIcon(String streamIcon) {
        this.streamIcon = streamIcon;
    }

    public String getEpgChannelId() {
        return epgChannelId;
    }

    public void setEpgChannelId(String epgChannelId) {
        this.epgChannelId = epgChannelId;
    }

    public String getAdded() {
        return added;
    }

    public void setAdded(String added) {
        this.added = added;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCustomSid() {
        return customSid;
    }

    public void setCustomSid(String customSid) {
        this.customSid = customSid;
    }

    public int getTvArchive() {
        return tvArchive;
    }

    public void setTvArchive(int tvArchive) {
        this.tvArchive = tvArchive;
    }

    public String getDirectSource() {
        return directSource;
    }

    public void setDirectSource(String directSource) {
        this.directSource = directSource;
    }

    public int getTvArchiveDuration() {
        return tvArchiveDuration;
    }

    public void setTvArchiveDuration(int tvArchiveDuration) {
        this.tvArchiveDuration = tvArchiveDuration;
    }
}
