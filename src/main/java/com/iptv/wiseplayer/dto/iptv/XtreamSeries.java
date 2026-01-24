package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class XtreamSeries {
    @JsonProperty("num")
    private int num;
    @JsonProperty("name")
    private String name;
    @JsonProperty("series_id")
    private int seriesId;
    @JsonProperty("cover")
    private String cover;
    @JsonProperty("plot")
    private String plot;
    @JsonProperty("cast")
    private String cast;
    @JsonProperty("director")
    private String director;
    @JsonProperty("genre")
    private String genre;
    @JsonProperty("releaseDate")
    private String releaseDate;
    @JsonProperty("last_modified")
    private String lastModified;
    @JsonProperty("rating")
    private String rating;
    @JsonProperty("category_id")
    private String categoryId;
}
