package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single live stream entry returned by the Xtream Codes
 * {@code get_live_streams} action.
 *
 * <p>Fields like {@code catchup}, {@code catchup_days} and {@code catchup_method}
 * are independent of {@code tv_archive}: a channel can support catch-up via a
 * provider-level method (e.g. Flussonic) even when {@code tv_archive=0}.
 */
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

    /** XMLTV channel identifier — used to correlate with EPG data. */
    @JsonProperty("epg_channel_id")
    private String epgChannelId;

    @JsonProperty("added")
    private String added;

    @JsonProperty("category_id")
    private String categoryId;

    /**
     * Multi-category membership list. A channel can belong to more than one
     * category. Always prefer this over the single {@code category_id} when
     * building category filters.
     */
    @JsonProperty("category_ids")
    private List<Integer> categoryIds = new ArrayList<>();

    @JsonProperty("custom_sid")
    private String customSid;

    /**
     * {@code 1} when the provider exposes an Xtream-style time-shift archive
     * for this channel, {@code 0} otherwise. Use in conjunction with
     * {@code catchup} — a channel may support catch-up via a different method
     * even when this is {@code 0}.
     */
    @JsonProperty("tv_archive")
    private int tvArchive;

    @JsonProperty("direct_source")
    private String directSource;

    /**
     * Number of days the Xtream-style archive covers. Only meaningful when
     * {@code tv_archive=1}.
     */
    @JsonProperty("tv_archive_duration")
    private int tvArchiveDuration;

    /**
     * {@code true} when the provider explicitly marks this channel as
     * catch-up capable (independent of {@code tv_archive}).
     */
    @JsonProperty("catchup")
    private boolean catchup;

    /**
     * Number of days of catch-up archive available as reported by the
     * provider's channel list. May be a numeric string or empty.
     */
    @JsonProperty("catchup_days")
    private String catchupDays;

    /**
     * Catch-up method code advertised by the provider — e.g. {@code "default"},
     * {@code "flussonic"}, {@code "shift"}, {@code "xc"}.
     */
    @JsonProperty("catchup_method")
    private String catchupMethod;

    /** {@code 1} when the channel is flagged as adult content. */
    @JsonProperty("is_adult")
    private int isAdult;

    /**
     * String form of the channel identifier, sometimes used instead of
     * the numeric {@code stream_id} for EPG lookups.
     */
    @JsonProperty("channel_id")
    private String channelId;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public int getStreamId() { return streamId; }
    public void setStreamId(int streamId) { this.streamId = streamId; }

    public String getStreamIcon() { return streamIcon; }
    public void setStreamIcon(String streamIcon) { this.streamIcon = streamIcon; }

    public String getEpgChannelId() { return epgChannelId; }
    public void setEpgChannelId(String epgChannelId) { this.epgChannelId = epgChannelId; }

    public String getAdded() { return added; }
    public void setAdded(String added) { this.added = added; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public List<Integer> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Integer> categoryIds) {
        this.categoryIds = categoryIds != null ? categoryIds : new ArrayList<>();
    }

    public String getCustomSid() { return customSid; }
    public void setCustomSid(String customSid) { this.customSid = customSid; }

    public int getTvArchive() { return tvArchive; }
    public void setTvArchive(int tvArchive) { this.tvArchive = tvArchive; }

    public String getDirectSource() { return directSource; }
    public void setDirectSource(String directSource) { this.directSource = directSource; }

    public int getTvArchiveDuration() { return tvArchiveDuration; }
    public void setTvArchiveDuration(int tvArchiveDuration) { this.tvArchiveDuration = tvArchiveDuration; }

    public boolean isCatchup() { return catchup; }
    public void setCatchup(boolean catchup) { this.catchup = catchup; }

    public String getCatchupDays() { return catchupDays; }
    public void setCatchupDays(String catchupDays) { this.catchupDays = catchupDays; }

    public String getCatchupMethod() { return catchupMethod; }
    public void setCatchupMethod(String catchupMethod) { this.catchupMethod = catchupMethod; }

    public int getIsAdult() { return isAdult; }
    public void setIsAdult(int isAdult) { this.isAdult = isAdult; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    /**
     * Returns the effective number of catch-up days from either the Xtream
     * archive duration or the per-channel {@code catchup_days} field. Returns
     * {@code null} when no value is available.
     */
    public Integer effectiveCatchupDays() {
        if (tvArchiveDuration > 0) {
            return tvArchiveDuration;
        }
        if (catchupDays != null && !catchupDays.isBlank()) {
            try {
                int d = Integer.parseInt(catchupDays.trim());
                return d > 0 ? d : null;
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    /**
     * Returns {@code true} when this channel has any form of catch-up support —
     * either via the Xtream time-shift archive ({@code tv_archive=1}) or via the
     * provider-level catch-up flag ({@code catchup=true}).
     */
    public boolean hasAnyCatchup() {
        return tvArchive == 1 || catchup;
    }
}

