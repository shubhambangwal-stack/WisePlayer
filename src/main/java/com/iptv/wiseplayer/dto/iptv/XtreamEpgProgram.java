package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single EPG entry returned by the Xtream Codes
 * {@code get_simple_data_table} / {@code get_short_epg} actions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XtreamEpgProgram {

    @JsonProperty("id")
    private String id;
    @JsonProperty("epg_id")
    private String epgId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("lang")
    private String lang;
    @JsonProperty("start")
    private long start;
    @JsonProperty("end")
    private long end;
    @JsonProperty("description")
    private String description;
    @JsonProperty("has_archive")
    private boolean hasArchive;
    @JsonProperty("channel_id")
    private String channelId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEpgId() {
        return epgId;
    }

    public void setEpgId(String epgId) {
        this.epgId = epgId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasArchive() {
        return hasArchive;
    }

    public void setHasArchive(boolean hasArchive) {
        this.hasArchive = hasArchive;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}