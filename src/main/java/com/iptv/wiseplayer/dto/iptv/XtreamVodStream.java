package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class XtreamVodStream {
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
    @JsonProperty("rating")
    private String rating;
    @JsonProperty("added")
    private String added;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("container_extension")
    private String containerExtension;
    @JsonProperty("direct_source")
    private String directSource;
}
