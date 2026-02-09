package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
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
}
