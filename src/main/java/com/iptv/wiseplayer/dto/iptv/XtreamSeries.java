package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

    public int getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(int seriesId) {
        this.seriesId = seriesId;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
